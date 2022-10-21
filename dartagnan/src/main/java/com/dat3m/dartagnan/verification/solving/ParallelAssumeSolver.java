package com.dat3m.dartagnan.verification.solving;

import com.dat3m.dartagnan.encoding.*;
import com.dat3m.dartagnan.program.Program;
import com.dat3m.dartagnan.program.analysis.BranchEquivalence;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.utils.Result;
import com.dat3m.dartagnan.verification.Context;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.wmm.Wmm;
import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;
import com.dat3m.dartagnan.wmm.axiom.Axiom;
import com.dat3m.dartagnan.wmm.Relation;
import com.dat3m.dartagnan.wmm.relation.RelationNameRepository;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.java_smt.SolverContextFactory;
import org.sosy_lab.java_smt.api.*;

import java.util.*;

import static com.dat3m.dartagnan.utils.Result.*;
import static java.util.Collections.singletonList;




public class ParallelAssumeSolver extends ModelChecker{




    private static final Logger logger = LogManager.getLogger(AssumeSolver.class);

    private final SolverContext mainCTX;
    private final ProverEnvironment mainProver;
    private final VerificationTask mainTask;

    private ParallelResultCollector resultCollector;
    private final FormulaQueueManager fqmgr;
    private final ShutdownManager sdm;
    private final SolverContextFactory.Solvers solver;
    private final Configuration solverConfig;

    private ParallelAssumeSolver(SolverContext ctx, ProverEnvironment prover, VerificationTask task, ShutdownManager sdm, SolverContextFactory.Solvers solver, Configuration solverConfig) {
        mainCTX = ctx;
        mainProver = prover;
        mainTask = task;
        this.sdm = sdm;
        this.fqmgr = new FormulaQueueManager();
        this.solver = solver;
        this.solverConfig = solverConfig;
    }

    public static ParallelAssumeSolver run(SolverContext mainCTX, ProverEnvironment prover, VerificationTask task, SolverContextFactory.Solvers solver, Configuration solverConfig,
                                   QueueType queueTypeSetting, int queueSettingInt1, int queueSettingInt2, int maxNumberOfThreads, ShutdownManager sdm)
            throws InterruptedException, SolverException, InvalidConfigurationException{
        ParallelAssumeSolver parallelAssumeSolver = new ParallelAssumeSolver(mainCTX, prover, task, sdm, solver, solverConfig);
        parallelAssumeSolver.run(queueTypeSetting, queueSettingInt1, queueSettingInt2, maxNumberOfThreads);
        return parallelAssumeSolver;
    }


    private void run(QueueType queueTypeSetting, int queueTypeSettingInt1, int queueTypeSettingInt2, int maxNumberOfThreads)
            throws InterruptedException, SolverException, InvalidConfigurationException {
        fqmgr.populateFormulaQueue(queueTypeSetting, queueTypeSettingInt1, queueTypeSettingInt2);

        resultCollector = new ParallelResultCollector(PASS, 0,maxNumberOfThreads, fqmgr.getQueueSize());

        Wmm memoryModel = mainTask.getMemoryModel();
        Context analysisContext = Context.create();
        Configuration config = mainTask.getConfig();

        memoryModel.configureAll(config);
        preprocessProgram(mainTask, config);
        performStaticProgramAnalyses(mainTask, analysisContext, config);
        performStaticWmmAnalyses(mainTask, analysisContext, config);


        context = EncodingContext.of(mainTask, analysisContext, mainCTX);

        WmmEncoder wmmEncoder = WmmEncoder.withContext(context);

        wmmEncoder.initializeEncoding(mainCTX);


        int totalThreadnumber = fqmgr.getQueueSize();


        List<Thread> threads = new ArrayList<Thread>(totalThreadnumber);


        switch(queueTypeSetting){
            case EMPTY:
            case SINGLE_LITERAL:
                System.out.println("EMPTY AND SINGLE_LITERAL are not implemented"); //TODO implement :)
            case RELATIONS_SORT:
            case RELATIONS_SHUFFLE:
            case MUTUALLY_EXCLUSIVE_SHUFFLE:
            case MUTUALLY_EXCLUSIVE_SORT:
            case EVENTS:
            case MUTUALLY_EXCLUSIVE_EVENTS:
                String relationName = RelationNameRepository.RF;
                fqmgr.setRelationName(relationName);
                Relation relation = mainTask.getMemoryModel().getRelation(relationName);
                RelationAnalysis relationAnalysis = context.getAnalysisContext().get(RelationAnalysis.class);
                RelationAnalysis.Knowledge knowledge = relationAnalysis.getKnowledge(relation);
                TupleSet rfEncodeSet = knowledge.getMaySet();
                List<Tuple> tupleList = new ArrayList<>(rfEncodeSet);
                fqmgr.setTupleList(tupleList);
        }

        logger.info("Starting Thread creation.");

        for(int i = 0; i < totalThreadnumber; i++) {
            synchronized (resultCollector){
                while(!resultCollector.canAddThread()){
                    if(resultCollector.getAggregatedResult().equals(FAIL)) {
                        // TODO: kill all threads
                        sdm.requestShutdown("Done");
                        logger.info("Parallel calculations ended. Result: FAIL");
                        resultCollector.printTimes();
                        res = resultCollector.getAggregatedResult();
                        return;
                    }
                    resultCollector.wait();
                }
            }
            int threadID = i;

            // Case 1: true
            try{
                threads.add(new Thread(()-> {
                    try {
                        runThread(analysisContext, threadID);
                    } catch (InterruptedException e){
                        logger.warn("Timeout elapsed. The SMT solver was stopped");
                        System.out.println("TIMEOUT");
                    } catch (Exception e) {
                        logger.error(e.getMessage());
                        System.out.println("ERROR");
                    }
                }));
                threads.get(threads.size()-1).start();
            } catch (Exception e) {
                logger.error(e.getMessage());
                System.out.println("ERROR");
            }
        }

        int loopcount = 0;
        while (true){

            logger.info("Mainloop: loop " + loopcount);
            loopcount++;
            synchronized(resultCollector){
                if(resultCollector.getAggregatedResult().equals(FAIL)){
                    // TODO: kill all threads
                    sdm.requestShutdown("Done");
                    /*for(Thread t:threads){
                        logger.info("tried to interrupt");
                        t.interrupt();
                    }*/
                    logger.info("Parallel calculations ended. Result: FAIL");
                    resultCollector.printTimes();
                    res = resultCollector.getAggregatedResult();
                    return;
                } else {

                    if (resultCollector.getNumberOfFinishedThreads() == totalThreadnumber) {//
                        logger.info("Parallel calculations ended. Result: UNKNOWN/PASS");
                        resultCollector.printTimes();
                        res = resultCollector.getAggregatedResult();
                        return;
                    }
                    logger.info("Mainloop: numberOfResults: " + resultCollector.getNumberOfFinishedThreads() + " totalThreadNumber: " + totalThreadnumber);

                    //TODO : check if threads are still alive
                    resultCollector.wait();
                }
            }
        }
    }

    private void runThread(Context analysisContext, int threadID)
            throws InterruptedException, SolverException, InvalidConfigurationException{
        ParallelAssumeThreadSolver myThreadSolver = new ParallelAssumeThreadSolver(mainTask, fqmgr, sdm, resultCollector, solver, solverConfig, analysisContext, threadID);
        myThreadSolver.run();
    }


    /*public static Result run(SolverContext mainCtx, ProverEnvironment prover, VerificationTask task, SolverContextFactory.Solvers solver,
                             ShutdownManager sdm, Configuration solverConfig, QueueType queueTypeSetting, int queueSettingInt1, int queueSettingInt2, int maxNumberOfThreads)
            throws InterruptedException, SolverException, InvalidConfigurationException {


        Program program = task.getProgram();
        Wmm memoryModel = task.getMemoryModel();
        Context analysisContext = Context.create();
        Configuration config = task.getConfig();

        memoryModel.configureAll(config);
        preprocessProgram(task, config);
        performStaticProgramAnalyses(task, analysisContext, config);
        performStaticWmmAnalyses(task, analysisContext, config);

        ProgramEncoder programEncoder = ProgramEncoder.fromConfig(program, analysisContext, config);
        PropertyEncoder propertyEncoder = PropertyEncoder.fromConfig(program, memoryModel,analysisContext, config);
        WmmEncoder wmmEncoder = WmmEncoder.fromConfig(memoryModel, analysisContext, config);
        SymmetryEncoder symmetryEncoder = SymmetryEncoder.fromConfig(memoryModel, analysisContext, config);

        programEncoder.initializeEncoding(mainCtx);
        propertyEncoder.initializeEncoding(mainCtx);
        wmmEncoder.initializeEncoding(mainCtx);
        symmetryEncoder.initializeEncoding(mainCtx);


        BooleanFormulaManager bmgr = mainCtx.getFormulaManager().getBooleanFormulaManager();

        BooleanFormula propertyEncoding = propertyEncoder.encodeSpecification(task.getProperty(), mainCtx);
        if(bmgr.isFalse(propertyEncoding)) {
            logger.info("Verification finished: property trivially holds");

            return PASS;

        }




        logger.info("Starting encoding using " + mainCtx.getVersion());

        FormulaContainer formulaContainer = new FormulaContainer();
        formulaContainer.setFullProgramFormula(programEncoder.encodeFullProgram(mainCtx));
        formulaContainer.setWmmFormula(wmmEncoder.encodeFullMemoryModel(mainCtx));
        formulaContainer.setWitnessFormula(task.getWitness().encode(task.getProgram(), mainCtx));
        formulaContainer.setSymmFormula(symmetryEncoder.encodeFullSymmetry(mainCtx));

        ThreadPackage mainThreadPackage = new ThreadPackage();
        mainThreadPackage.setProverEnvironment(prover);
        mainThreadPackage.setSolverContext(mainCtx);
        mainThreadPackage.setPropertyEncoding(propertyEncoding);


        String relationName = RelationNameRepository.RF;
        Relation rf = task.getMemoryModel().getRelationRepository().getRelation(relationName);
		TupleSet rfEncodeSet = rf.getEncodeTupleSet();
		List<Tuple> tupleList = new ArrayList<>(rfEncodeSet);

        Set<Event> branchRepresentatives = analysisContext.get(BranchEquivalence.class).getAllRepresentatives();
        List<Event> eventList = new ArrayList<>(branchRepresentatives);


        FormulaQueueManager fqmgr = new FormulaQueueManager(mainCtx, task, analysisContext);
        switch(queueTypeSetting){
            case RELATIONS_SHUFFLE:
                Collections.shuffle(tupleList);
                fqmgr.relationTuples(queueSettingInt1, queueSettingInt2, tupleList, relationName);
                break;

            case RELATIONS_SORT:
                sort(tupleList, task);
                fqmgr.relationTuples(queueSettingInt1, queueSettingInt2, tupleList, relationName);
                break;

            case MUTUALLY_EXCLUSIVE_SHUFFLE:
                Collections.shuffle(tupleList);
                fqmgr.relationTuplesMutuallyExlusive(queueSettingInt1, queueSettingInt2, tupleList, relationName);
                break;

            case MUTUALLY_EXCLUSIVE_SORT:
                sort(tupleList, task);
                fqmgr.relationTuplesMutuallyExlusive(queueSettingInt1, queueSettingInt2, tupleList, relationName);
                break;

            case EVENTS:
                fqmgr.eventsQueue(queueSettingInt1, queueSettingInt2, eventList);
                break;

            case MUTUALLY_EXCLUSIVE_EVENTS:
                fqmgr.mutuallyExclusiveEventsQueue(queueSettingInt1, queueSettingInt2, eventList);
                break;


            case EMPTY:
                fqmgr.createTrues(queueSettingInt1);
                break;


            case SINGLE_LITERAL:
                //not implemented again
                return UNKNOWN;

        }

        int totalThreadnumber = fqmgr.getQueueSize();


        List<Thread> threads = new ArrayList<Thread>(totalThreadnumber);

        ParallelResultCollector resultCollector = new ParallelResultCollector(PASS, 0, maxNumberOfThreads, totalThreadnumber);

        for(int i = 0; i < totalThreadnumber; i++) {
            synchronized (resultCollector){
                while(!resultCollector.canAddThread()){
                    resultCollector.wait();
                }
            }
            int threadID1 = i;
            BooleanFormula checkFormula = fqmgr.getNextFormula();
            // Case 1: true
			try{
                threads.add(new Thread(()-> {
                    try {
                        runThread(mainThreadPackage, task, checkFormula,
                                threadID1, resultCollector, formulaContainer,
                                sdm, solverConfig, solver, propertyEncoder);
                    } catch (InterruptedException e){
                        logger.warn("Timeout elapsed. The SMT solver was stopped");
                        System.out.println("TIMEOUT");
                    } catch (Exception e) {
                        logger.error(e.getMessage());
                        System.out.println("ERROR");
                    }
                }));
                threads.get(threads.size()-1).start();
			} catch (Exception e) {
                logger.error(e.getMessage());
                System.out.println("ERROR");
            }
		}

        int loopcount = 0;
        while (true){

            logger.info("Mainloop: loop " + loopcount);
            loopcount++;
            synchronized(resultCollector){
                if(resultCollector.getAggregatedResult().equals(FAIL)){
                    // TODO: kill all threads
                    sdm.requestShutdown("Done");
                    /*for(Thread t:threads){
                        logger.info("tried to interrupt");
                        t.interrupt();
                    }*//*
                    logger.info("Parallel calculations ended. Result: FAIL");
                    resultCollector.printTimes();
                    return resultCollector.getAggregatedResult();
                } else {

                        if (resultCollector.getNumberOfFinishedThreads() == totalThreadnumber) {//
                            logger.info("Parallel calculations ended. Result: UNKNOWN/PASS");
                            resultCollector.printTimes();
                            return resultCollector.getAggregatedResult();
                        }
                        logger.info("Mainloop: numberOfResults: " + resultCollector.getNumberOfFinishedThreads() + " totalThreadNumber: " + totalThreadnumber);

                    //TODO : check if threads are still alive
                    resultCollector.wait();
                }
            }
        }

	}*/

    /*private synchronized static void populateThreadPackage(ThreadPackage threadPackage, ThreadPackage mainThreadPackage, FormulaContainer mainThreadFormulas,
                                               ShutdownManager sdm, Configuration solverConfig, SolverContextFactory.Solvers solver)
        throws InvalidConfigurationException, InterruptedException{

        SolverContext ctx = mainThreadPackage.getSolverContext();

        SolverContext newSolverContext = SolverContextFactory.createSolverContext(
                solverConfig,
                BasicLogManager.create(solverConfig),
                sdm.getNotifier(),
                solver);
        threadPackage.setSolverContext(newSolverContext);

        BooleanFormula newPropertyEncoding = newSolverContext.getFormulaManager().translateFrom(mainThreadPackage.getPropertyEncoding(), ctx.getFormulaManager());

        threadPackage.setPropertyEncoding(newPropertyEncoding);

        ProverEnvironment newProverEnvironment = newSolverContext.newProverEnvironment(SolverContext.ProverOptions.GENERATE_MODELS);


        threadPackage.setProverEnvironment(newProverEnvironment);

        newProverEnvironment.addConstraint(newSolverContext.getFormulaManager().translateFrom(mainThreadFormulas.getFullProgramFormula(), ctx.getFormulaManager()));
        newProverEnvironment.addConstraint(newSolverContext.getFormulaManager().translateFrom(mainThreadFormulas.getWmmFormula(), ctx.getFormulaManager()));
        newProverEnvironment.addConstraint(newSolverContext.getFormulaManager().translateFrom(mainThreadFormulas.getWitnessFormula(), ctx.getFormulaManager()));
        newProverEnvironment.addConstraint(newSolverContext.getFormulaManager().translateFrom(mainThreadFormulas.getSymmFormula(), ctx.getFormulaManager()));
    }*/


	/*private static void runThread(ThreadPackage mainThreadPackage, VerificationTask task, BooleanFormula threadFormula,
                                  int threadID, ParallelResultCollector resultCollector,  FormulaContainer mainThreadFormulas,
                                  ShutdownManager sdm, Configuration solverConfig, SolverContextFactory.Solvers solver, PropertyEncoder propertyEncoder)
			throws InterruptedException, SolverException, InvalidConfigurationException {

        long startTime = System.currentTimeMillis();
        ThreadPackage myThreadPackage = new ThreadPackage();
        populateThreadPackage(myThreadPackage, mainThreadPackage, mainThreadFormulas, sdm, solverConfig, solver);


        Result res;
        SolverContext ctx = myThreadPackage.getSolverContext();
        SolverContext mainCtx;
        synchronized(mainThreadPackage){mainCtx = mainThreadPackage.getSolverContext();}
        ProverEnvironment prover = myThreadPackage.getProverEnvironment();
        BooleanFormula propertyEncoding = myThreadPackage.getPropertyEncoding();


        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();

		// note BooleanFormula var = task.getMemoryModel().getRelationRepository().getRelation(RF).getSMTVar(tuple,ctx);
		// note prover.addConstraint(assumption ? var : bmgr.not(var));



        synchronized (mainCtx){prover.addConstraint(ctx.getFormulaManager().translateFrom(threadFormula, mainCtx.getFormulaManager()));}

        BooleanFormula assumptionLiteral = bmgr.makeVariable("DAT3M_spec_assumption");

        BooleanFormula assumedSpec = bmgr.implication(assumptionLiteral, propertyEncoding);

        prover.addConstraint(assumedSpec);


        logger.info("Thread " + threadID + ": Starting first solver.check()");
        if(prover.isUnsatWithAssumptions(singletonList(assumptionLiteral))) {

            synchronized (mainCtx) {prover.addConstraint(ctx.getFormulaManager().translateFrom(propertyEncoder.encodeBoundEventExec(mainCtx),mainCtx.getFormulaManager()));}

            //prover.addConstraint(propertyEncoder.encodeBoundEventExec(ctx));

            logger.info("Thread " + threadID + ": Starting second solver.check()");
            res = prover.isUnsat()? PASS : Result.UNKNOWN;
        } else {
            res = FAIL;
        }
    
        if(logger.isDebugEnabled()) {        	
    		String smtStatistics = "\n ===== SMT Statistics ===== \n";
    		for(String key : prover.getStatistics().keySet()) {
    			smtStatistics += String.format("\t%s -> %s\n", key, prover.getStatistics().get(key));
    		}
    		logger.debug(smtStatistics);
        }

        res = task.getProgram().getAss().getInvert() ? res.invert() : res;





        logger.info("Thread " + threadID + ": Verification finished with result " + res);
        logger.info("Thread " + threadID + ": My Formula was: " + threadFormula);

        prover.close();
        ctx.close();
        synchronized (resultCollector){
            if(resultCollector.getAggregatedResult().equals(FAIL)){
                return;
            }
            resultCollector.updateResult(res, threadID, startTime);
            resultCollector.notify();

        }

    }*/

    /*static private void sort(List<Tuple> row, VerificationTask task) {
        /*if (!breakBySyncDegree) {
            // ===== Natural order =====
            row.sort(Comparator.naturalOrder());
            return;
        }*//*

        // ====== Sync-degree based order ======

        // Setup of data structures
        Set<Event> inEvents = new HashSet<>();
        Set<Event> outEvents = new HashSet<>();
        for (Tuple t : row) {
            inEvents.add(t.getFirst());
            outEvents.add(t.getSecond());
        }

        Map<Event, Integer> combInDegree = new HashMap<>(inEvents.size());
        Map<Event, Integer> combOutDegree = new HashMap<>(outEvents.size());

        List<Axiom> axioms = task.getMemoryModel().getAxioms();
        for (Event e : inEvents) {
            int syncDeg = axioms.stream()
                    .mapToInt(ax -> ax.getRelation().getMinTupleSet().getBySecond(e).size() + 1).max().orElse(0);
            combInDegree.put(e, syncDeg);
        }
        for (Event e : outEvents) {
            int syncDec = axioms.stream()
                    .mapToInt(ax -> ax.getRelation().getMinTupleSet().getByFirst(e).size() + 1).max().orElse(0);
            combOutDegree.put(e, syncDec);
        }

        // Sort by sync degrees
        row.sort(Comparator.<Tuple>comparingInt(t -> combInDegree.get(t.getFirst()) * combOutDegree.get(t.getSecond())).reversed());
    }*/




}
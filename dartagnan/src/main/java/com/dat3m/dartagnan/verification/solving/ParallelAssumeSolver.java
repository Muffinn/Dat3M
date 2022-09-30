package com.dat3m.dartagnan.verification.solving;

import com.dat3m.dartagnan.encoding.ProgramEncoder;
import com.dat3m.dartagnan.encoding.PropertyEncoder;
import com.dat3m.dartagnan.encoding.SymmetryEncoder;
import com.dat3m.dartagnan.encoding.WmmEncoder;
import com.dat3m.dartagnan.program.analysis.BranchEquivalence;
import com.dat3m.dartagnan.program.analysis.ExecutionAnalysis;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.utils.Result;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.wmm.axiom.Axiom;
import com.dat3m.dartagnan.wmm.relation.Relation;
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
import static com.dat3m.dartagnan.wmm.relation.RelationNameRepository.CO;
import static java.util.Collections.singletonList;




public class ParallelAssumeSolver {




    private static final Logger logger = LogManager.getLogger(AssumeSolver.class);

    /**
     * Solver function with Default Settings for the queue population
     */
    public static Result run(SolverContext ctx, ProverEnvironment prover, VerificationTask task, SolverContextFactory.Solvers solver,
                             ShutdownManager sdm, Configuration solverConfig)
            throws InterruptedException, SolverException, InvalidConfigurationException {
        return run(ctx, prover, task,solver, sdm, solverConfig, QueueType.RELATIONS_SHUFFLE, 5, 2, 6);
    }


    public static Result run(SolverContext ctx, ProverEnvironment prover, VerificationTask task, SolverContextFactory.Solvers solver,
                             ShutdownManager sdm, Configuration solverConfig, QueueType queueTypeSetting, int queueSettingInt1, int queueSettingInt2, int maxNumberOfThreads)
            throws InterruptedException, SolverException, InvalidConfigurationException {



		task.preprocessProgram();
		task.performStaticProgramAnalyses();
		task.performStaticWmmAnalyses();


        task.initializeEncoders(ctx);
        ProgramEncoder programEncoder = task.getProgramEncoder();
        PropertyEncoder propertyEncoder = task.getPropertyEncoder();
        WmmEncoder wmmEncoder = task.getWmmEncoder();
        SymmetryEncoder symmEncoder = task.getSymmetryEncoder();

        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();

        BooleanFormula propertyEncoding = propertyEncoder.encodeSpecification(task.getProperty(), ctx);
        if(bmgr.isFalse(propertyEncoding)) {
            logger.info("Verification finished: property trivially holds");

            return PASS;

        }
        ExecutionAnalysis exec = task.getAnalysisContext().get(ExecutionAnalysis.class);
        //exec.areMutuallyExclusive();




        logger.info("Starting encoding using " + ctx.getVersion());

        FormulaContainer formulaContainer = new FormulaContainer();
        formulaContainer.setFullProgramFormula(programEncoder.encodeFullProgram(ctx));
        formulaContainer.setWmmFormula(wmmEncoder.encodeFullMemoryModel(ctx));
        formulaContainer.setWitnessFormula(task.getWitness().encode(task.getProgram(), ctx));
        formulaContainer.setSymmFormula(symmEncoder.encodeFullSymmetry(ctx));

        ThreadPackage mainThreadPackage = new ThreadPackage();
        mainThreadPackage.setProverEnvironment(prover);
        mainThreadPackage.setSolverContext(ctx);
        mainThreadPackage.setPropertyEncoding(propertyEncoding);


        String relationName = RelationNameRepository.RF;
        Relation rf = task.getMemoryModel().getRelationRepository().getRelation(relationName);
		TupleSet rfEncodeSet = rf.getEncodeTupleSet();
		List<Tuple> tupleList = new ArrayList<>(rfEncodeSet);

        Set<Event> branchRepresentatives = task.getAnalysisContext().get(BranchEquivalence.class).getAllRepresentatives();
        List<Event> eventList = new ArrayList<>(branchRepresentatives);


        FormulaQueueManager fqmgr = new FormulaQueueManager(ctx, task);
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

        //int numberOfRelations = Math.min(3, tupleList.size());
        int totalThreadnumber = fqmgr.getQueuesize();



        /*ThreadPackage[] threadPackages = new ThreadPackage[totalThreadnumber];
        for (int i = 0; i < totalThreadnumber; i++){
            threadPackages[i] = new ThreadPackage();
        }*/

        List<Thread> threads = new ArrayList<Thread>(totalThreadnumber);

        ParallelResultCollector resultCollector = new ParallelResultCollector(PASS, 0, maxNumberOfThreads, totalThreadnumber);

        for(int i = 0; i < totalThreadnumber; i++) {
            synchronized (resultCollector){
                while(!resultCollector.canAddThread()){
                    resultCollector.wait();
                }
            }

            int threadID1 = i;
            ThreadPackage newthreadPackage = new ThreadPackage();
            //populateThreadPackage(newthreadPackage, mainThreadPackage, formulaContainer, sdm, solverConfig, solver);
            //BooleanFormula wasExecuted = branchRepresentatives.iterator().next().exec();  //note hier geht's weiter gleich weiter
            BooleanFormula checkFormula = fqmgr.getNextFormula();


            // Case 1: true
			try{
                threads.add(new Thread(()-> {
                    try {
                        runThread(mainThreadPackage, task, checkFormula,
                                threadID1, resultCollector, formulaContainer,
                                sdm, solverConfig, solver);
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
			// Case 2: false
			/*int threadID2 = i * 2 + 1;


            populateThreadPackage(threadPackages[threadID2], mainThreadPackage, formulaContainer, sdm, solverConfig, solver);
            BooleanFormula wasNotExecuted = bmgr.not(branchRepresentatives.iterator().next().exec());  //note hier geht's weiter gleich weiter


            threads.add(new Thread(()->{try{runThread(threadPackages[threadID2], mainThreadPackage, task, wasNotExecuted, threadID2, resultCollector);
                } catch (InterruptedException e){
                    logger.warn("Timeout elapsed. The SMT solver was stopped");
                    System.out.println("TIMEOUT");
                } catch (Exception e) {
                    logger.error(e.getMessage());
                    System.out.println("ERROR");
                }
            }));
            threads.get(threads.size() - 1).start();*/
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

	}

    private synchronized static void populateThreadPackage(ThreadPackage threadPackage, ThreadPackage mainThreadPackage, FormulaContainer mainThreadFormulas,
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

        ProverEnvironment newProverEnvironment = newSolverContext.newProverEnvironment();

        threadPackage.setProverEnvironment(newProverEnvironment);

        newProverEnvironment.addConstraint(newSolverContext.getFormulaManager().translateFrom(mainThreadFormulas.getFullProgramFormula(), ctx.getFormulaManager()));
        newProverEnvironment.addConstraint(newSolverContext.getFormulaManager().translateFrom(mainThreadFormulas.getWmmFormula(), ctx.getFormulaManager()));
        newProverEnvironment.addConstraint(newSolverContext.getFormulaManager().translateFrom(mainThreadFormulas.getWitnessFormula(), ctx.getFormulaManager()));
        newProverEnvironment.addConstraint(newSolverContext.getFormulaManager().translateFrom(mainThreadFormulas.getSymmFormula(), ctx.getFormulaManager()));
    }


	private static void runThread(ThreadPackage mainThreadPackage, VerificationTask task, BooleanFormula threadFormula,
                                  int threadID, ParallelResultCollector resultCollector,  FormulaContainer mainThreadFormulas,
                                  ShutdownManager sdm, Configuration solverConfig, SolverContextFactory.Solvers solver)
			throws InterruptedException, SolverException, InvalidConfigurationException {

        long startTime = System.currentTimeMillis();
        ThreadPackage myThreadPackage = new ThreadPackage();
        populateThreadPackage(myThreadPackage, mainThreadPackage, mainThreadFormulas, sdm, solverConfig, solver);


        Result res;
        SolverContext ctx = myThreadPackage.getSolverContext();
        SolverContext mainCtx = mainThreadPackage.getSolverContext();
        ProverEnvironment prover = myThreadPackage.getProverEnvironment();
        BooleanFormula propertyEncoding = myThreadPackage.getPropertyEncoding();


        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
        PropertyEncoder propertyEncoder = task.getPropertyEncoder();

		// note BooleanFormula var = task.getMemoryModel().getRelationRepository().getRelation(RF).getSMTVar(tuple,ctx);
		// note prover.addConstraint(assumption ? var : bmgr.not(var));



        prover.addConstraint(ctx.getFormulaManager().translateFrom(threadFormula, mainCtx.getFormulaManager()));

        BooleanFormula assumptionLiteral = bmgr.makeVariable("DAT3M_spec_assumption");

        BooleanFormula assumedSpec = bmgr.implication(assumptionLiteral, propertyEncoding);

        prover.addConstraint(assumedSpec);


        logger.info("Thread " + threadID + ": Starting first solver.check()");
        if(prover.isUnsatWithAssumptions(singletonList(assumptionLiteral))) {

            prover.addConstraint(ctx.getFormulaManager().translateFrom(propertyEncoder.encodeBoundEventExec(mainCtx),mainCtx.getFormulaManager()));

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

    }

    static private void sort(List<Tuple> row, VerificationTask task) {
        /*if (!breakBySyncDegree) {
            // ===== Natural order =====
            row.sort(Comparator.naturalOrder());
            return;
        }*/

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
    }



    public enum QueueType{
        RELATIONS_SHUFFLE, RELATIONS_SORT, SINGLE_LITERAL, EMPTY, MUTUALLY_EXCLUSIVE_SORT, MUTUALLY_EXCLUSIVE_SHUFFLE, EVENTS, MUTUALLY_EXCLUSIVE_EVENTS;
    }
}
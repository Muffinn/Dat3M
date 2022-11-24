package com.dat3m.dartagnan.verification.solving;

import com.dat3m.dartagnan.encoding.*;
import com.dat3m.dartagnan.utils.Result;
import com.dat3m.dartagnan.verification.Context;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.java_smt.SolverContextFactory;
import org.sosy_lab.java_smt.api.*;

import java.util.*;

import static com.dat3m.dartagnan.utils.Result.FAIL;
import static com.dat3m.dartagnan.utils.Result.PASS;
import static java.util.Collections.singletonList;


public class ParallelAssumeThreadSolver extends ModelChecker{




    private static final Logger logger = LogManager.getLogger(ParallelRefinementThreadSolver.class);

    private final SolverContext myCTX;
    private final ProverEnvironment myProver;
    private final VerificationTask mainTask;

    private final ParallelResultCollector mainResultCollector;
    private final SplittingManager mainFQMGR;
    private final ShutdownManager sdm;
    private final Context mainAnalysisContext;
    private final int myThreadID;

    private final ThreadStatisticManager myStatisticManager;

    public ParallelAssumeThreadSolver(VerificationTask task, SplittingManager mainFQMGR, ShutdownManager sdm,
                                      ParallelResultCollector mainResultCollector, SolverContextFactory.Solvers solver, Configuration solverConfig, Context mainAnalysisContext, int threadID)
            throws InterruptedException, SolverException, InvalidConfigurationException{
        myCTX = SolverContextFactory.createSolverContext(
                solverConfig,
                BasicLogManager.create(solverConfig),
                sdm.getNotifier(),
                solver);
        myProver = myCTX.newProverEnvironment(SolverContext.ProverOptions.GENERATE_MODELS);
        mainTask = task;
        this.sdm = sdm;
        this.mainFQMGR = mainFQMGR;
        this.mainResultCollector = mainResultCollector;
        this.mainAnalysisContext = mainAnalysisContext;
        myThreadID = threadID;
        myStatisticManager = new ThreadStatisticManager(myThreadID);
    }




    public void run()
            throws InterruptedException, SolverException, InvalidConfigurationException{

        long startTime = System.currentTimeMillis();

        logger.info("Thread " + myThreadID + ": " + "ThreadSolver Run starts");


        ProgramEncoder programEncoder;
        PropertyEncoder propertyEncoder;
        WmmEncoder wmmEncoder;
        SymmetryEncoder symmetryEncoder;
        BooleanFormula propertyEncoding;
        synchronized(mainTask){context = EncodingContext.of(mainTask, mainAnalysisContext, myCTX);
        programEncoder = ProgramEncoder.withContext(context);
        propertyEncoder = PropertyEncoder.withContext(context);
        wmmEncoder = WmmEncoder.withContext(context);

        symmetryEncoder = SymmetryEncoder.withContext(context, mainTask.getMemoryModel(), mainAnalysisContext);

        programEncoder.initializeEncoding(myCTX);
        propertyEncoder.initializeEncoding(myCTX);
        wmmEncoder.initializeEncoding(myCTX);
        symmetryEncoder.initializeEncoding(myCTX);
        propertyEncoding = propertyEncoder.encodeSpecification();}




        logger.info("Thread " + myThreadID + ": " +  "Starting encoding using " + myCTX.getVersion());
        myProver.addConstraint(programEncoder.encodeFullProgram());
        myProver.addConstraint(wmmEncoder.encodeFullMemoryModel());

        // For validation this contains information.
        // For verification graph.encode() just returns ctx.mkTrue()
        synchronized(mainTask){myProver.addConstraint(mainTask.getWitness().encode(this.context));}

        myProver.addConstraint(symmetryEncoder.encodeFullSymmetryBreaking());


        BooleanFormulaManager bmgr = myCTX.getFormulaManager().getBooleanFormulaManager();
        BooleanFormula assumptionLiteral = bmgr.makeVariable("DAT3M_spec_assumption");
        BooleanFormula assumedSpec = bmgr.implication(assumptionLiteral, propertyEncoding);
        myProver.addConstraint(assumedSpec);

        //------------myformula-Generation------------
        /*QueueType queueType = mainFQMGR.getQueueType();
        BooleanFormula myFormula  = myCTX.getFormulaManager().getBooleanFormulaManager().makeTrue();
        switch (queueType){
            case RELATIONS_SORT:
            case RELATIONS_SHUFFLE:
            case SINGLE_LITERAL:
            case EVENTS:
            case MUTUALLY_EXCLUSIVE_EVENTS:
            case EMPTY:
            case MUTUALLY_EXCLUSIVE_SORT:
            case MUTUALLY_EXCLUSIVE_SHUFFLE:


        }*/
        BooleanFormula myFormula = mainFQMGR.generateRelationFormula(myCTX, context, mainTask, myThreadID, myStatisticManager);
        myProver.addConstraint(myFormula);
        //----------------------------------------


        logger.info("Thread " + myThreadID + ": " +  "Starting first solver.check()");
        if(myProver.isUnsatWithAssumptions(singletonList(assumptionLiteral))) {
            myProver.addConstraint(propertyEncoder.encodeBoundEventExec());
            logger.info("Thread " + myThreadID + ": " +  "Starting second solver.check()");
            res = myProver.isUnsat()? PASS : Result.UNKNOWN;
        } else {
            res = FAIL;
        }

        if(logger.isDebugEnabled()) {
            String smtStatistics = "\n ===== SMT Statistics ===== \n";
            for(String key : myProver.getStatistics().keySet()) {
                smtStatistics += String.format("\t%s -> %s\n", key, myProver.getStatistics().get(key));
            }
            logger.debug(smtStatistics);
        }
        res = mainTask.getProgram().getAss().getInvert() ? res.invert() : res;

        synchronized(mainResultCollector){
            mainResultCollector.updateResult(res, myThreadID, myStatisticManager);
            mainResultCollector.notify();
        }
        myStatisticManager.reportResult(res);
        logger.info("Thread " + myThreadID + ": " +  "Verification finished with result " + res);


    }



    private BooleanFormula generateRelationFormula(int queueInt1, int queueInt2, BitSet myBitSet, List<Tuple> tupleList, EncodingContext myContext){
        int positiveCount = 0;
        BooleanFormulaManager bmgr = myCTX.getFormulaManager().getBooleanFormulaManager();
        BooleanFormula myFormula = bmgr.makeTrue();
        String relationName = mainFQMGR.getRelationName();
        for (int i = 0; i < queueInt1; i++){
            if(myBitSet.get(i)){
                BooleanFormula var = mainTask.getMemoryModel().getRelation(relationName).getSMTVar(tupleList.get(i), myContext);
                myFormula = bmgr.and(var, myFormula);
                positiveCount++;
                if(positiveCount == queueInt2){
                    logger.info("Thread " + myThreadID + ": " +  "generated Formula: " + myFormula);
                    return myFormula;
                }
            }else{
                BooleanFormula notVar = bmgr.not(mainTask.getMemoryModel().getRelation(relationName).getSMTVar(tupleList.get(i), myContext));
                myFormula = bmgr.and(notVar, myFormula);
            }
        }
        logger.info("Thread " + myThreadID + ": " +  "generated Formula: " + myFormula);
        return myFormula;

    }


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
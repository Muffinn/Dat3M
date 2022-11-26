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


public class ParallelAssumeThreadSolver extends ParallelThreadSolver{

    private static final Logger logger = LogManager.getLogger(ParallelRefinementThreadSolver.class);

    private final Context mainAnalysisContext;

    public ParallelAssumeThreadSolver(VerificationTask task, SplittingManager mainSPMGR, ShutdownManager sdm,
                                      ParallelResultCollector mainResultCollector, SolverContextFactory.Solvers solver,
                                      Configuration solverConfig, Context mainAnalysisContext, int threadID,
                                      ParallelSolverConfiguration parallelConfig, ThreadStatisticManager myStatisticManager)
            throws InterruptedException, SolverException, InvalidConfigurationException{

        super(task, mainSPMGR, sdm, mainResultCollector, solver, solverConfig, threadID, parallelConfig, myStatisticManager);
        this.mainAnalysisContext = mainAnalysisContext;

    }


    public void run()
            throws InterruptedException, SolverException, InvalidConfigurationException{

        myStatisticManager.reportStart();
        logger.info("Thread " + myThreadID + ": " + "ThreadSolver Run starts");

        //--------------------Encoder------------------------

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


        //...........................Encoding.................................
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

        fetchSplittingObjects();
        BooleanFormula myFormula = generateMyFormula();
        myProver.addConstraint(myFormula);


        //-------------------Solver---------------------
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
}
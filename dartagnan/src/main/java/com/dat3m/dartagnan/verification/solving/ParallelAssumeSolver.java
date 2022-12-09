package com.dat3m.dartagnan.verification.solving;

import com.dat3m.dartagnan.encoding.*;
import com.dat3m.dartagnan.verification.Context;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.wmm.Wmm;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.java_smt.SolverContextFactory;
import org.sosy_lab.java_smt.api.*;


public class ParallelAssumeSolver extends AbstractParallelSolver {

    private static final Logger logger = LogManager.getLogger(AssumeSolver.class);

    protected Context analysisContext;


    public ParallelAssumeSolver(SolverContext ctx, ProverEnvironment prover, VerificationTask task, ShutdownManager sdm,
                                SolverContextFactory.Solvers solver, Configuration solverConfig,
                                ParallelSolverConfiguration parallelConfig)
            throws InvalidConfigurationException{
        super(ctx, prover, task, sdm, solver, solverConfig, parallelConfig);
    }

    public static ParallelAssumeSolver run(SolverContext mainCTX, ProverEnvironment prover, VerificationTask task, SolverContextFactory.Solvers solver, Configuration solverConfig,
                                           ShutdownManager sdm, ParallelSolverConfiguration parallelConfig)
            throws InterruptedException, SolverException, InvalidConfigurationException{
        ParallelAssumeSolver parallelAssumeSolver = new ParallelAssumeSolver(mainCTX, prover, task, sdm, solver, solverConfig, parallelConfig);
        parallelAssumeSolver.run();
        return parallelAssumeSolver;
    }


    protected void run()
            throws InterruptedException, SolverException, InvalidConfigurationException {

        statisticManager.reportStart();

        Wmm memoryModel = mainTask.getMemoryModel();
        analysisContext = Context.create();
        Configuration config = mainTask.getConfig();

        memoryModel.configureAll(config);
        preprocessProgram(mainTask, config);
        performStaticProgramAnalyses(mainTask, analysisContext, config);
        performStaticWmmAnalyses(mainTask, analysisContext, config);

        context = EncodingContext.of(mainTask, analysisContext, mainCTX);
        WmmEncoder wmmEncoder = WmmEncoder.withContext(context);
        wmmEncoder.initializeEncoding(mainCTX);


        fillSplittingManager(analysisContext, mainTask);



        //......................Start..Threads...........
        logger.info("Starting Thread creation.");
        startThreads();

        //......................Start..Wait..Loop........
        resultWaitLoop();
    }


    protected void runThread(int threadID)
            throws InterruptedException, SolverException, InvalidConfigurationException{
        ParallelAssumeThreadSolver myThreadSolver = new ParallelAssumeThreadSolver(mainTask, spmgr, sdm, resultCollector,
                solverType, solverConfig, analysisContext, threadID, parallelConfig,
                statisticManager.getThreadStatisticManager(threadID));
        myThreadSolver.run();
    }






}
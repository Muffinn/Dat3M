package com.dat3m.dartagnan.verification.solving;

import com.dat3m.dartagnan.encoding.ProgramEncoder;
import com.dat3m.dartagnan.encoding.PropertyEncoder;
import com.dat3m.dartagnan.encoding.SymmetryEncoder;
import com.dat3m.dartagnan.encoding.WmmEncoder;
import com.dat3m.dartagnan.utils.Result;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.wmm.relation.Relation;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;

import static com.dat3m.dartagnan.utils.Result.*;
import static com.dat3m.dartagnan.wmm.relation.RelationNameRepository.CO;
import static com.dat3m.dartagnan.wmm.relation.RelationNameRepository.RF;
import static java.util.Collections.singletonList;



    /*public synchronized void pass() {
        threadCounter++;
    }
    public synchronized void fail() {
        threadCounter++;
        endRes = FAIL;
    }
    public synchronized void unknown() {
        threadCounter++;
        endRes = UNKNOWN;
    }*/


public class ParallelAssumeSolver {




    private static final Logger logger = LogManager.getLogger(AssumeSolver.class);

    public static Result run(SolverContext ctx, ProverEnvironment prover, VerificationTask task, SolverContextFactory.Solvers solver,
                             ShutdownManager sdm, Configuration solverConfig)
            throws InterruptedException, SolverException, InvalidConfigurationException {

        parallelResultCollector resultCollector = new parallelResultCollector(PASS, 0);

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

        logger.info("Starting encoding using " + ctx.getVersion());
        BooleanFormula fullProgramFormula = programEncoder.encodeFullProgram(ctx);
        //prover.addConstraint(programEncoder.encodeFullProgram(ctx));
        BooleanFormula wmmFormula = wmmEncoder.encodeFullMemoryModel(ctx);
        //prover.addConstraint(wmmEncoder.encodeFullMemoryModel(ctx));
        // For validation this contains information.
        // For verification graph.encode() just returns ctx.mkTrue()
        BooleanFormula witnessFormula = task.getWitness().encode(task.getProgram(), ctx);
        //prover.addConstraint(task.getWitness().encode(task.getProgram(), ctx));
        BooleanFormula symmFormula = symmEncoder.encodeFullSymmetry(ctx);
        //prover.addConstraint(symmEncoder.encodeFullSymmetry(ctx));


		Relation rf = task.getMemoryModel().getRelationRepository().getRelation(RF);
		TupleSet rfEncodeSet = rf.getMaxTupleSet();
		List<Tuple> tupleList = new ArrayList<>(rfEncodeSet);
		Collections.shuffle(tupleList);

        int numberOfRelations = Math.min(10, tupleList.size());
        //boolean finishedArray[][] = new boolean[numberOfRelations][2];
        //Result results[][] = new Result[numberOfRelations][2];

        ProverEnvironment[] proverEnvironments = new ProverEnvironment[numberOfRelations * 2];
        SolverContext[] solverContexts = new SolverContext[numberOfRelations * 2];
        BooleanFormula[] propertyEncodings = new BooleanFormula[numberOfRelations * 2];


        for(int i = 0; i < numberOfRelations; i++) {
			Tuple tuple = tupleList.get(i);
            int threadID1 = i * 2;

            solverContexts[threadID1] = SolverContextFactory.createSolverContext(
                    solverConfig,
                    BasicLogManager.create(solverConfig),
                    sdm.getNotifier(),
                    solver);

            propertyEncodings[threadID1] = solverContexts[threadID1].getFormulaManager().translateFrom(propertyEncoding, ctx.getFormulaManager());
            proverEnvironments[threadID1] = solverContexts[threadID1].newProverEnvironment();
            proverEnvironments[threadID1].addConstraint(solverContexts[threadID1].getFormulaManager().translateFrom(fullProgramFormula, ctx.getFormulaManager()));
            proverEnvironments[threadID1].addConstraint(solverContexts[threadID1].getFormulaManager().translateFrom(wmmFormula, ctx.getFormulaManager()));
            proverEnvironments[threadID1].addConstraint(solverContexts[threadID1].getFormulaManager().translateFrom(witnessFormula, ctx.getFormulaManager()));
            proverEnvironments[threadID1].addConstraint(solverContexts[threadID1].getFormulaManager().translateFrom(symmFormula, ctx.getFormulaManager()));

            // Case 1: true
			try{
                new Thread(()-> {
                    try {
                        runThread(solverContexts[threadID1], proverEnvironments[threadID1], task, tuple, true,
                                threadID1, resultCollector, propertyEncodings[threadID1]);
                    } catch (InterruptedException e){
                        logger.warn("Timeout elapsed. The SMT solver was stopped");
                        System.out.println("TIMEOUT");
                    } catch (Exception e) {
                        logger.error(e.getMessage());
                        System.out.println("ERROR");
                    }
                }).start();
			} catch (Exception e) {
                logger.error(e.getMessage());
                System.out.println("ERROR");
            }
			// Case 2: false
			int threadID2 = i * 2 + 1;

            solverContexts[threadID2] = SolverContextFactory.createSolverContext(
                    solverConfig,
                    BasicLogManager.create(solverConfig),
                    sdm.getNotifier(),
                    solver);

            propertyEncodings[threadID2] = solverContexts[threadID2].getFormulaManager().translateFrom(propertyEncoding, ctx.getFormulaManager());
            proverEnvironments[threadID2] = solverContexts[threadID2].newProverEnvironment();

            proverEnvironments[threadID2].addConstraint(solverContexts[threadID2].getFormulaManager().translateFrom(fullProgramFormula, ctx.getFormulaManager()));
            proverEnvironments[threadID2].addConstraint(solverContexts[threadID2].getFormulaManager().translateFrom(wmmFormula, ctx.getFormulaManager()));
            proverEnvironments[threadID2].addConstraint(solverContexts[threadID2].getFormulaManager().translateFrom(witnessFormula, ctx.getFormulaManager()));
            proverEnvironments[threadID2].addConstraint(solverContexts[threadID2].getFormulaManager().translateFrom(symmFormula, ctx.getFormulaManager()));



            new Thread(()->{try{runThread(solverContexts[threadID2], proverEnvironments[threadID2], task, tuple,
                    false, threadID2, resultCollector, propertyEncodings[threadID2]);
                } catch (InterruptedException e){
                    logger.warn("Timeout elapsed. The SMT solver was stopped");
                    System.out.println("TIMEOUT");
                } catch (Exception e) {
                    logger.error(e.getMessage());
                    System.out.println("ERROR");
                }
            }).start();
			// Case 2.1: w wird nicht ausgeführt
			// Case 2.2: r wird nicht ausgeführt
			// Case 2.3: w und r, aber addresse unterschiedlich
			// Case 2.4: w und r und loc, aber r liest von einem anderen Store
		}

        int loopcount = 0;
        while (true){

            logger.info("Mainloop: loop " + loopcount);
            loopcount++;
            synchronized(resultCollector){
                if(resultCollector.getAggregatedResult().equals(FAIL)){
                    // TODO: kill all threads
                    return resultCollector.getAggregatedResult();
                } else {

                        if (resultCollector.getNumberOfFinishedThreads() == numberOfRelations * 2) {//
                            return resultCollector.getAggregatedResult();
                        }
                        logger.info("Mainloop: numberOfResults: " + resultCollector.getAggregatedResult() + " numberOfRelations" + numberOfRelations);

                    //TODO : check if threads are still alive
                    resultCollector.wait();
                }
            }
        }

	}

	private static void runThread(SolverContext ctx, ProverEnvironment prover, VerificationTask task, Tuple tuple, boolean assumption,
                                  int threadID, parallelResultCollector resultCollector, BooleanFormula propertyEncoding)
			throws InterruptedException, SolverException, InvalidConfigurationException {
		Result res;

        BooleanFormulaManager bmgr;
        bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
        PropertyEncoder propertyEncoder = task.getPropertyEncoder();


		BooleanFormula var = task.getMemoryModel().getRelationRepository().getRelation(RF).getSMTVar(tuple,ctx);


		prover.addConstraint(assumption ? var : bmgr.not(var));
        BooleanFormula assumptionLiteral = bmgr.makeVariable("DAT3M_spec_assumption");

        BooleanFormula assumedSpec = bmgr.implication(assumptionLiteral, propertyEncoding);


        prover.addConstraint(assumedSpec);


        logger.info("Thread " + threadID + ": Starting first solver.check()");
        if(prover.isUnsatWithAssumptions(singletonList(assumptionLiteral))) {
            prover.addConstraint(propertyEncoder.encodeBoundEventExec(ctx));
            logger.info("Starting second solver.check()");
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
        synchronized (resultCollector){
            if(resultCollector.getAggregatedResult().equals(FAIL)){
                return;
            }
            resultCollector.updateResult(res);
            resultCollector.notify();

        }

    }
}
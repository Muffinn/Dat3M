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
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.java_smt.api.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;

import static com.dat3m.dartagnan.utils.Result.*;
import static com.dat3m.dartagnan.wmm.relation.RelationNameRepository.CO;
import static com.dat3m.dartagnan.wmm.relation.RelationNameRepository.RF;
import static java.util.Collections.singletonList;

public class ParallelAssumeSolver {




    private static final Logger logger = LogManager.getLogger(AssumeSolver.class);

    public static Result run(SolverContext ctx, ProverEnvironment prover, VerificationTask task)
            throws InterruptedException, SolverException, InvalidConfigurationException {
		Result res = Result.PASS;
        Integer numberOfResults = 0;

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
        System.out.println("EncodeSet size Affe " + rfEncodeSet.size());
		List<Tuple> tupleList = new ArrayList<>(rfEncodeSet);
		Collections.shuffle(tupleList);

        int numberOfRelations = Math.min(15, tupleList.size());
        //boolean finishedArray[][] = new boolean[numberOfRelations][2];
        //Result results[][] = new Result[numberOfRelations][2];
        System.out.println("number of Relations Affe:" + numberOfRelations);

        ProverEnvironment[] proverEnvironments = new ProverEnvironment[numberOfRelations * 2];


        for(int i = 0; i < numberOfRelations; i++) {
			Tuple tuple = tupleList.get(i);
            int threadID1 = i * 2;
            proverEnvironments[threadID1] = ctx.newProverEnvironment();
            proverEnvironments[threadID1].addConstraint(fullProgramFormula);
            proverEnvironments[threadID1].addConstraint(wmmFormula);
            proverEnvironments[threadID1].addConstraint(witnessFormula);
            proverEnvironments[threadID1].addConstraint(symmFormula);
			// Case 1: true
			try{
                new Thread(()-> {
                    try {
                        runThread(ctx, proverEnvironments[threadID1], task, tuple, true, threadID1, numberOfResults, res, propertyEncoding);
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
            proverEnvironments[threadID2] = ctx.newProverEnvironment();
            proverEnvironments[threadID2].addConstraint(fullProgramFormula);
            proverEnvironments[threadID2].addConstraint(wmmFormula);
            proverEnvironments[threadID2].addConstraint(witnessFormula);
            proverEnvironments[threadID2].addConstraint(symmFormula);


			new Thread(()->{try{runThread(ctx,proverEnvironments[threadID2], task, tuple,false, threadID2, numberOfResults, res,propertyEncoding);
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


        while (true){
            System.out.println("LoopAffe");
            synchronized(res){
                if(res == FAIL){
                    // TODO: kill all threads
                    return res;
                } else {
                    synchronized (numberOfResults) {
                        if (numberOfResults == numberOfRelations * 2) {//
                            return res;
                        }
                    }
                    res.wait();
                }
            }
        }

	}

	private static void runThread(SolverContext ctx, ProverEnvironment prover, VerificationTask task, Tuple tuple, boolean assumption,
                                  int threadID, Integer numberOfResults, Result endRes, BooleanFormula propertyEncoding)
			throws InterruptedException, SolverException, InvalidConfigurationException {
		Result res = Result.UNKNOWN;
        System.out.println("ThreadAFFE" + threadID + assumption);

		/*task.initializeEncoders(ctx);
        ProgramEncoder programEncoder = task.getProgramEncoder();
        PropertyEncoder propertyEncoder = task.getPropertyEncoder();
        WmmEncoder wmmEncoder = task.getWmmEncoder();
        SymmetryEncoder symmEncoder = task.getSymmetryEncoder();

		BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();


		BooleanFormula propertyEncoding = propertyEncoder.encodeSpecification(task.getProperty(), ctx);
        if(bmgr.isFalse(propertyEncoding)) {
            logger.info("Verification finished: property trivially holds");
       		synchronized (endRes){
                       endRes = PASS;
                       synchronized (numberOfResults){
                           numberOfResults++;
                       }
                       endRes.notify();
            }
            return;
        }

        logger.info("Starting encoding using " + ctx.getVersion());
        prover.addConstraint(programEncoder.encodeFullProgram(ctx));
        prover.addConstraint(wmmEncoder.encodeFullMemoryModel(ctx));
        // For validation this contains information.
        // For verification graph.encode() just returns ctx.mkTrue()
        prover.addConstraint(task.getWitness().encode(task.getProgram(), ctx));
        prover.addConstraint(symmEncoder.encodeFullSymmetry(ctx));
        */

        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
        PropertyEncoder propertyEncoder = task.getPropertyEncoder();

		BooleanFormula var = task.getMemoryModel().getRelationRepository().getRelation(RF).getSMTVar(tuple,ctx);
		prover.addConstraint(assumption ? var : bmgr.not(var));

        BooleanFormula assumptionLiteral = bmgr.makeVariable("DAT3M_spec_assumption");
        BooleanFormula assumedSpec = bmgr.implication(assumptionLiteral, propertyEncoding);
        prover.addConstraint(assumedSpec);
        
        logger.info("Starting first solver.check()");
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
        logger.info("Verification finished with result " + res);
        synchronized (endRes){
            if(endRes == FAIL){
                return;
            }
            if (res == FAIL){
                endRes = FAIL;
                endRes.notify();
            }
            if (res == UNKNOWN){
                endRes = UNKNOWN;
                synchronized (numberOfResults){
                    numberOfResults++;
                }
                endRes.notify();
            }
            if(res == PASS){
                synchronized (numberOfResults){
                    numberOfResults++;
                }
                endRes.notify();
            }
        }
        return;
        //return res;
    }
}
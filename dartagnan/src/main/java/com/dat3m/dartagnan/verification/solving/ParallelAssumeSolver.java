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

import static com.dat3m.dartagnan.utils.Result.*;
import static com.dat3m.dartagnan.wmm.relation.RelationNameRepository.RF;
import static java.util.Collections.singletonList;

public class ParallelAssumeSolver {

    private static final Logger logger = LogManager.getLogger(AssumeSolver.class);

    public static Result run(SolverContext ctx, ProverEnvironment prover, VerificationTask task)
            throws InterruptedException, SolverException, InvalidConfigurationException {
		Result res = Result.UNKNOWN;

		task.preprocessProgram();
		task.performStaticProgramAnalyses();
		task.performStaticWmmAnalyses();

		Relation rf = task.getMemoryModel().getRelationRepository().getRelation(RF);
		TupleSet rfEncodeSet = rf.getEncodeTupleSet();
		List<Tuple> toupleList = new ArrayList<>(rfEncodeSet);
		Collections.shuffle(toupleList);
        int numberOfRelations = Math.min(15, toupleList.size());
        boolean finishedArray[][] = new boolean[numberOfRelations][2];

        Result results[][] = new Result[numberOfRelations][2];
		for(int i = 0; i < numberOfRelations; i++) {
			Tuple tuple = toupleList.get(i);
            int threadID = i;
			// Case 1: true
			try(ProverEnvironment prover1 = ctx.newProverEnvironment()) {
				new Thread(()-> {
                    try {
                        runThread(ctx, prover1, task, tuple, true, threadID, finishedArray, results);
                    } catch (InterruptedException e){
                        logger.warn("Timeout elapsed. The SMT solver was stopped");
                        System.out.println("TIMEOUT");
                        System.exit(0);
                    } catch (Exception e) {
                        logger.error(e.getMessage());
                        System.out.println("ERROR");
                        System.exit(1);
                    }
                }).start();
			} catch (Exception e) {
                logger.error(e.getMessage());
                System.out.println("ERROR");
                System.exit(1);
            }
			// Case 2: false
			ProverEnvironment prover2 = ctx.newProverEnvironment();

			new Thread(()->{try{runThread(ctx,prover2,task,tuple,false, threadID, finishedArray, results);
                } catch (InterruptedException e){
                    logger.warn("Timeout elapsed. The SMT solver was stopped");
                    System.out.println("TIMEOUT");
                    System.exit(0);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                    System.out.println("ERROR");
                    System.exit(1);
                }
            }).start();
			// Case 2.1: w wird nicht ausgeführt
			// Case 2.2: r wird nicht ausgeführt
			// Case 2.3: w und r, aber addresse unterschiedlich
			// Case 2.4: w und r und loc, aber r liest von einem anderen Store
		}
        while (true){
            for (int i = 0; i < numberOfRelations; i++){
                synchronized(finishedArray){
                    if(finishedArray[i][0] && finishedArray[i][1]) {
                        synchronized (results){
                            if(results[i][0] == FAIL || results[i][1] == FAIL){
                                res = FAIL;
                                return res;
                            }
                            if(results[i][0] == UNKNOWN || results[i][1] == UNKNOWN){
                                res = UNKNOWN;
                                return res;
                            }
                            res = PASS;
                        }

                        return res;
                    }
                }
                Thread.sleep(10);
            }
        }
	}

	private static void runThread(SolverContext ctx, ProverEnvironment prover, VerificationTask task, Tuple tuple, boolean assumption,
                                  int   id, boolean[][] finishedArray, Result[][] results)
			throws InterruptedException, SolverException, InvalidConfigurationException {
		Result res = Result.UNKNOWN;

		task.initializeEncoders(ctx);
        ProgramEncoder programEncoder = task.getProgramEncoder();
        PropertyEncoder propertyEncoder = task.getPropertyEncoder();
        WmmEncoder wmmEncoder = task.getWmmEncoder();
        SymmetryEncoder symmEncoder = task.getSymmetryEncoder();

		BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();

		BooleanFormula propertyEncoding = propertyEncoder.encodeSpecification(task.getProperty(), ctx);
        if(bmgr.isFalse(propertyEncoding)) {
            logger.info("Verification finished: property trivially holds");
       		synchronized (finishedArray){
                   synchronized (results){
                       if (assumption){
                           finishedArray[id][1] = true;
                           results[id][1] = PASS;
                       } else {
                           finishedArray[id][0] = true;
                           results[id][0] = PASS;
                       }
                   }
            }
            //return PASS;
            return;
        }

        logger.info("Starting encoding using " + ctx.getVersion());
        prover.addConstraint(programEncoder.encodeFullProgram(ctx));
        prover.addConstraint(wmmEncoder.encodeFullMemoryModel(ctx));
        // For validation this contains information.
        // For verification graph.encode() just returns ctx.mkTrue()
        prover.addConstraint(task.getWitness().encode(task.getProgram(), ctx));
        prover.addConstraint(symmEncoder.encodeFullSymmetry(ctx));

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
        synchronized (finishedArray){
            synchronized (results){
                if (assumption){
                    finishedArray[id][1] = true;
                    results[id][1] = res;
                } else {
                    finishedArray[id][0] = true;
                    results[id][0] = res;
                }
            }
        }
        return;
        //return res;
    }
}
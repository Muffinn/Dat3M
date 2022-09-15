package com.dat3m.dartagnan.verification.solving;

import ap.Prover;
import com.dat3m.dartagnan.encoding.ProgramEncoder;
import com.dat3m.dartagnan.encoding.PropertyEncoder;
import com.dat3m.dartagnan.encoding.SymmetryEncoder;
import com.dat3m.dartagnan.encoding.WmmEncoder;
import com.dat3m.dartagnan.program.analysis.BranchEquivalence;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.utils.Result;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import com.google.common.collect.ClassToInstanceMap;
import com.microsoft.z3.Solver;
import com.sun.jdi.event.EventSet;
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
import java.util.Set;
import java.util.concurrent.Semaphore;

import static com.dat3m.dartagnan.utils.Result.*;
import static com.dat3m.dartagnan.wmm.relation.RelationNameRepository.CO;
import static com.dat3m.dartagnan.wmm.relation.RelationNameRepository.RF;
import static java.util.Collections.singletonList;




public class ParallelAssumeSolver {




    private static final Logger logger = LogManager.getLogger(AssumeSolver.class);


    public static Result run(SolverContext ctx, ProverEnvironment prover, VerificationTask task, SolverContextFactory.Solvers solver,
                             ShutdownManager sdm, Configuration solverConfig)
            throws InterruptedException, SolverException, InvalidConfigurationException {

        ParallelResultCollector resultCollector = new ParallelResultCollector(PASS, 0);

		task.preprocessProgram();
		task.performStaticProgramAnalyses();
		task.performStaticWmmAnalyses();

        Set<Event> branchRepresentatives = task.getAnalysisContext().get(BranchEquivalence.class).getAllRepresentatives();





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

        FormulaContainer formulaContainer = new FormulaContainer();

        formulaContainer.setFullProgramFormula(programEncoder.encodeFullProgram(ctx));
        formulaContainer.setWmmFormula(wmmEncoder.encodeFullMemoryModel(ctx));
        formulaContainer.setWitnessFormula(task.getWitness().encode(task.getProgram(), ctx));
        formulaContainer.setSymmFormula(symmEncoder.encodeFullSymmetry(ctx));


		Relation rf = task.getMemoryModel().getRelationRepository().getRelation(CO);
		TupleSet rfEncodeSet = rf.getMinTupleSet();
		List<Tuple> tupleList = new ArrayList<>(rfEncodeSet);
		Collections.shuffle(tupleList);

        int numberOfRelations = Math.min(3, tupleList.size());

        ThreadPackage mainThreadPackage = new ThreadPackage();
        mainThreadPackage.setProverEnvironment(prover);
        mainThreadPackage.setSolverContext(ctx);
        mainThreadPackage.setPropertyEncoding(propertyEncoding);
        ThreadPackage[] threadPackages = new ThreadPackage[numberOfRelations * 2];
        for (int i = 0; i < numberOfRelations * 2; i++){
            threadPackages[i] = new ThreadPackage();
        }

        List<Thread> threads = new ArrayList<Thread>(numberOfRelations * 2);



        for(int i = 0; i < numberOfRelations; i++) {
			Tuple tuple = tupleList.get(i);
            int threadID1 = i * 2;

            populateThreadPackage(threadPackages[threadID1], mainThreadPackage, formulaContainer, sdm, solverConfig, solver);
            BooleanFormula wasExecuted = branchRepresentatives.iterator().next().exec();  //note hier geht's weiter gleich weiter


            // Case 1: true
			try{
                threads.add(new Thread(()-> {
                    try {
                        runThread(threadPackages[threadID1], mainThreadPackage, task, wasExecuted,
                                threadID1, resultCollector);
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
			int threadID2 = i * 2 + 1;

            sdm.requestShutdown("");

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
            threads.get(threads.size() - 1).start();
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
                    sdm.requestShutdown("Done");
                    /*for(Thread t:threads){
                        logger.info("tried to interrupt");
                        t.interrupt();
                    }*/
                    logger.info("Parallel calculations ended. Result: FAIL");
                    return resultCollector.getAggregatedResult();
                } else {

                        if (resultCollector.getNumberOfFinishedThreads() == numberOfRelations * 2) {//
                            logger.info("Parallel calculations ended. Result: UNKNOWN/PASS");
                            return resultCollector.getAggregatedResult();
                        }
                        logger.info("Mainloop: numberOfResults: " + resultCollector.getAggregatedResult() + " numberOfRelations" + numberOfRelations);

                    //TODO : check if threads are still alive
                    resultCollector.wait();
                }
            }
        }

	}

    private static void populateThreadPackage(ThreadPackage threadPackage, ThreadPackage mainThreadPackage, FormulaContainer mainThreadFormulas,
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


	private static void runThread(ThreadPackage threadPackage , ThreadPackage mainThreadPackage, VerificationTask task, BooleanFormula threadFormula,
                                  int threadID, ParallelResultCollector resultCollector)
			throws InterruptedException, SolverException {

        Result res;
        SolverContext ctx = threadPackage.getSolverContext();
        SolverContext mainCtx = mainThreadPackage.getSolverContext();
        ProverEnvironment prover = threadPackage.getProverEnvironment();
        BooleanFormula propertyEncoding = threadPackage.getPropertyEncoding();


        BooleanFormulaManager bmgr;
        bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
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

            logger.info("Thread " + threadID + "Starting second solver.check()");
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
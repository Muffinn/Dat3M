package com.dat3m.dartagnan.verification.solving;

import com.dat3m.dartagnan.configuration.Baseline;
import com.dat3m.dartagnan.program.analysis.BranchEquivalence;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.verification.Context;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.wmm.Relation;
import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;
import com.dat3m.dartagnan.wmm.relation.RelationNameRepository;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.java_smt.SolverContextFactory;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext;
import org.sosy_lab.java_smt.api.SolverException;

import java.util.*;
import java.util.stream.Collectors;

import static com.dat3m.dartagnan.configuration.OptionNames.BASELINE;
import static com.dat3m.dartagnan.utils.Result.FAIL;
import static com.dat3m.dartagnan.utils.Result.PASS;

/*
    Refinement is a custom solving procedure that starts from a weak memory model (possibly the empty model)
    and iteratively refines it to perform a verification task.
    It can be understood as a lazy offline-SMT solver.
    More concretely, it iteratively:
        - Finds some assertion-violating execution w.r.t. to some (very weak) baseline memory model
        - Checks the consistency of this execution using a custom theory solver (CAAT-Solver)
        - Refines the used memory model if the found execution was inconsistent, using the explanations
          provided by the theory solver.
 */
@Options
public abstract class AbstractParallelSolver extends ModelChecker {

    protected static final Logger logger = LogManager.getLogger(AbstractParallelSolver.class);

    protected final SolverContext mainCTX;
    protected final ProverEnvironment mainProver;
    protected final VerificationTask mainTask;


    protected final ParallelResultCollector resultCollector;
    protected final SplittingManager spmgr;
    protected final ShutdownManager sdm;
    protected final SolverContextFactory.Solvers solverType;
    protected final Configuration solverConfig;
    protected final ParallelSolverConfiguration parallelConfig;

    protected final MainStatisticManager statisticManager;

    public AbstractParallelSolver(SolverContext c, ProverEnvironment p, VerificationTask t, ShutdownManager sdm, SolverContextFactory.Solvers solverType, Configuration solverConfig, ParallelSolverConfiguration parallelConfig)
            throws InvalidConfigurationException{
        mainCTX = c;
        mainProver = p;
        mainTask = t;
        this.sdm = sdm;
        this.spmgr = new SplittingManager(parallelConfig);
        this.solverType = solverType;
        this.solverConfig = solverConfig;
        this.parallelConfig = parallelConfig;
        this.statisticManager = new MainStatisticManager(parallelConfig.getNumberOfSplits(), parallelConfig, spmgr);
        this.resultCollector = new ParallelResultCollector(PASS, parallelConfig);
    }


    // =========================== Configurables ===========================

    @Option(name=BASELINE,
            description="Refinement starts from this baseline WMM.",
            secure=true,
            toUppercase=true)
    private EnumSet<Baseline> baselines = EnumSet.noneOf(Baseline.class);

    // ======================================================================

    abstract protected void run() throws InterruptedException, SolverException, InvalidConfigurationException;
    abstract protected void runThread(int threadID) throws InterruptedException, SolverException, InvalidConfigurationException;


    protected void fillSplittingManager(Context analysisContext, VerificationTask myTask){
        switch (parallelConfig.getSplittingObjectType()){
            case CO_RELATION_SPLITTING_OBJECTS:
                String relationCOName = RelationNameRepository.CO;
                spmgr.setRelationName(relationCOName);
                Relation relationCO = myTask.getMemoryModel().getRelation(relationCOName);
                RelationAnalysis relationAnalysisCO = context.getAnalysisContext().get(RelationAnalysis.class);
                RelationAnalysis.Knowledge knowledgeCO = relationAnalysisCO.getKnowledge(relationCO);
                TupleSet coEncodeSet = knowledgeCO.getMaySet();
                List<Tuple> tupleListCO = new ArrayList<>(coEncodeSet);
                spmgr.setTupleList(tupleListCO);
                spmgr.orderTuples();
                spmgr.filterTuples(context.getAnalysisContext());
                break;
            case RF_RELATION_SPLITTING_OBJECTS:
                String relationRFName = RelationNameRepository.RF;
                spmgr.setRelationName(relationRFName);
                Relation relationRF = myTask.getMemoryModel().getRelation(relationRFName);
                RelationAnalysis relationAnalysisRF = context.getAnalysisContext().get(RelationAnalysis.class);
                RelationAnalysis.Knowledge knowledge = relationAnalysisRF.getKnowledge(relationRF);
                TupleSet rfEncodeSet = knowledge.getMaySet();
                List<Tuple> tupleListRF = new ArrayList<>(rfEncodeSet);
                spmgr.setTupleList(tupleListRF);
                spmgr.orderTuples();
                spmgr.filterTuples(context.getAnalysisContext());
                break;
            case BRANCH_EVENTS_SPLITTING_OBJECTS:
                BranchEquivalence branchEquivalence = context.getAnalysisContext().get(BranchEquivalence.class);
                Set<Event> initialClass = branchEquivalence.getInitialClass();
                List<Event> branchEventList = branchEquivalence.getAllEquivalenceClasses().stream().filter(c -> c!=initialClass).map(c -> c.getRepresentative()).collect(Collectors.toList());
                spmgr.setEventList(branchEventList);
                spmgr.orderEvents(context.getAnalysisContext(), myTask);
                spmgr.filterEvents(context.getAnalysisContext());
                break;
            case ALL_EVENTS_SPLITTING_OBJECTS:
                List<Event> allEventList = myTask.getProgram().getEvents();
                spmgr.setEventList(allEventList);
                spmgr.orderEvents(context.getAnalysisContext(), myTask);
                spmgr.filterEvents(context.getAnalysisContext());
                break;
            case NO_SPLITTING_OBJECTS:
                break;

            default:
                throw(new Error("Formula Type " + parallelConfig.getSplittingStyle().name() +" is not supported in fillSplittingManager."));
        }
    }


    protected void startThreads()
    throws InterruptedException{
        int totalThreadNumber = parallelConfig.getNumberOfSplits();//spmgr.getQueueSize(); note error source?
        List<Thread> threads = new ArrayList<Thread>(totalThreadNumber);

        logger.info("Starting Thread creation.");

        for (int i = 0; i < totalThreadNumber; i++) {
            Thread.sleep(1);
            synchronized (resultCollector) {
                while (!resultCollector.canAddThread()) {
                    if (resultCollector.getAggregatedResult().equals(FAIL)) {
                        // TODO: kill all threads
                        sdm.requestShutdown("Done");
                        logger.info("Parallel calculations ended. Result: FAIL");
                        statisticManager.printStatistics();
                        res = resultCollector.getAggregatedResult();
                        return;
                    }
                    resultCollector.wait();
                }
            }
            int threadID = i;

            try {
                threads.add(new Thread(() -> {
                    try {
                        runThread(threadID);
                    } catch (InterruptedException e) {
                        logger.warn("Timeout elapsed. The SMT solver was stopped");
                        System.out.println("TIMEOUT");
                    } catch (Exception e) {
                        logger.error("Thread " + threadID + ": " + e.getMessage());
                        System.out.println("ERROR");
                    }
                }));
                threads.get(threads.size() - 1).start();
            } catch (Exception e) {
                logger.error(e.getMessage());
                System.out.println("ERROR");
            }
        }
    }

    protected void resultWaitLoop()
    throws InterruptedException{
        int totalThreadNumber = parallelConfig.getNumberOfSplits();
        int loopcount = 0;
        while (true){

            logger.info("Mainloop: loop " + loopcount);
            loopcount++;
            synchronized(resultCollector){
                if(resultCollector.getAggregatedResult().equals(FAIL)){
                    // TODO: kill all threads
                    sdm.requestShutdown("Done");
                    logger.info("Parallel calculations ended. Result: FAIL");
                    res = resultCollector.getAggregatedResult();
                    statisticManager.reportResult(res);
                    statisticManager.printStatistics();

                    return;
                } else {

                    if (resultCollector.getNumberOfFinishedThreads() == totalThreadNumber) {//
                        logger.info("Parallel calculations ended. Result: UNKNOWN/PASS");
                        res = resultCollector.getAggregatedResult();
                        statisticManager.reportResult(res);
                        statisticManager.printStatistics();

                        return;
                    }
                    logger.info("MainLoop: numberOfResults: " + resultCollector.getNumberOfFinishedThreads() + " totalThreadNumber: " + totalThreadNumber);

                    //TODO : check if threads are still alive
                    resultCollector.wait();
                }
            }
        }

    }
}


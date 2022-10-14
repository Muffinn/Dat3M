package com.dat3m.dartagnan.verification.solving;

import com.dat3m.dartagnan.configuration.Baseline;
import com.dat3m.dartagnan.encoding.ProgramEncoder;
import com.dat3m.dartagnan.encoding.PropertyEncoder;
import com.dat3m.dartagnan.encoding.SymmetryEncoder;
import com.dat3m.dartagnan.encoding.WmmEncoder;
import com.dat3m.dartagnan.program.Program;
import com.dat3m.dartagnan.program.analysis.BranchEquivalence;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.solver.caat.CAATSolver;
import com.dat3m.dartagnan.solver.caat4wmm.Refiner;
import com.dat3m.dartagnan.solver.caat4wmm.WMMSolver;
import com.dat3m.dartagnan.solver.caat4wmm.coreReasoning.CoreLiteral;
import com.dat3m.dartagnan.solver.caat4wmm.coreReasoning.RelLiteral;
import com.dat3m.dartagnan.utils.Result;
import com.dat3m.dartagnan.utils.logic.Conjunction;
import com.dat3m.dartagnan.utils.logic.DNF;
import com.dat3m.dartagnan.verification.Context;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.verification.model.EventData;
import com.dat3m.dartagnan.verification.model.ExecutionModel;
import com.dat3m.dartagnan.wmm.Wmm;
import com.dat3m.dartagnan.wmm.axiom.Acyclic;
import com.dat3m.dartagnan.wmm.axiom.Axiom;
import com.dat3m.dartagnan.wmm.axiom.Empty;
import com.dat3m.dartagnan.wmm.axiom.ForceEncodeAxiom;
import com.dat3m.dartagnan.wmm.relation.RecursiveRelation;
import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.relation.RelationNameRepository;
import com.dat3m.dartagnan.wmm.relation.base.stat.RelCartesian;
import com.dat3m.dartagnan.wmm.relation.base.stat.RelFencerel;
import com.dat3m.dartagnan.wmm.relation.base.stat.RelSetIdentity;
import com.dat3m.dartagnan.wmm.relation.binary.RelComposition;
import com.dat3m.dartagnan.wmm.relation.binary.RelIntersection;
import com.dat3m.dartagnan.wmm.relation.binary.RelMinus;
import com.dat3m.dartagnan.wmm.relation.binary.RelUnion;
import com.dat3m.dartagnan.wmm.utils.RelationRepository;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.java_smt.SolverContextFactory;
import org.sosy_lab.java_smt.api.*;

import java.util.*;
import java.util.function.BiPredicate;

import static com.dat3m.dartagnan.GlobalSettings.REFINEMENT_GENERATE_GRAPHVIZ_DEBUG_FILES;
import static com.dat3m.dartagnan.configuration.OptionNames.BASELINE;
import static com.dat3m.dartagnan.solver.caat.CAATSolver.Status.INCONCLUSIVE;
import static com.dat3m.dartagnan.solver.caat.CAATSolver.Status.INCONSISTENT;
import static com.dat3m.dartagnan.utils.Result.*;
import static com.dat3m.dartagnan.utils.visualization.ExecutionGraphVisualizer.generateGraphvizFile;
import static com.dat3m.dartagnan.wmm.relation.RelationNameRepository.*;

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
public class ParallelRefinementSolver extends ModelChecker {

    private static final Logger logger = LogManager.getLogger(ParallelRefinementSolver.class);

    private final SolverContext ctx;
    private final ProverEnvironment prover;
    private final VerificationTask task;
    private ParallelResultCollector resultCollector;
    private final QueueType queueTypeSetting;
    private final int queueSettingInt1;
    private final int queueSettingInt2;
    private final int maxNumberOfThreads;
    private ShutdownManager sdm;

    // =========================== Configurables ===========================

    @Option(name=BASELINE,
            description="Refinement starts from this baseline WMM.",
            secure=true,
            toUppercase=true)
    private EnumSet<Baseline> baselines = EnumSet.noneOf(Baseline.class);

    // ======================================================================

    private ParallelRefinementSolver(SolverContext c, ProverEnvironment p, VerificationTask t, ShutdownManager sdm,  QueueType queueTypeSetting, int queueSettingInt1, int queueSettingInt2, int maxNumberOfThreads) {
        ctx = c;
        prover = p;
        task = t;
        resultCollector = null;
        this.queueTypeSetting = queueTypeSetting;
        this.queueSettingInt1 = queueSettingInt1;
        this.queueSettingInt2 = queueSettingInt2;
        this.maxNumberOfThreads = maxNumberOfThreads;
        this.sdm = sdm;
    }

    //TODO: We do not yet use Witness information. The problem is that WitnessGraph.encode() generates
    // constraints on hb, which is not encoded in Refinement.
    //TODO (2): Add possibility for Refinement to handle CAT-properties (it ignores them for now).
    //run Method with default QueueSetting
    public static Result run(SolverContext ctx, ProverEnvironment prover, VerificationTask task, ShutdownManager sdm)
            throws InterruptedException, SolverException, InvalidConfigurationException {
        ParallelRefinementSolver solver = new ParallelRefinementSolver(ctx, prover, task, sdm, QueueType.EMPTY, 1, 1, 1);
        task.getConfig().inject(solver);
        logger.info("{}: {}", BASELINE, solver.baselines);
        return solver.run();
    }


    //run Method with custom QueueSetting
    public static Result run(SolverContext ctx, ProverEnvironment prover, VerificationTask task, ShutdownManager sdm, QueueType queueTypeSetting, int queueSettingInt1, int queueSettingInt2, int maxNumberOfThreads)
            throws InterruptedException, SolverException, InvalidConfigurationException {
        ParallelRefinementSolver solver = new ParallelRefinementSolver(ctx, prover, task, sdm, queueTypeSetting, queueSettingInt1, queueSettingInt2, maxNumberOfThreads);
        task.getConfig().inject(solver);
        logger.info("{}: {}", BASELINE, solver.baselines);
        return solver.run();
    }

    private Result run() throws InterruptedException, SolverException, InvalidConfigurationException {

        Program program = task.getProgram();
        Wmm memoryModel = task.getMemoryModel();
        Wmm baselineModel = createDefaultWmm();
        Context analysisContext = Context.create();
        Configuration config = task.getConfig();
        VerificationTask baselineTask = VerificationTask.builder()
                .withConfig(task.getConfig()).build(program, baselineModel, task.getProperty());//note maybe nur einer

        preprocessProgram(task, config);
        // We cut the rhs of differences to get a semi-positive model, if possible.
        // This call modifies the baseline model!
        Set<Relation> cutRelations = cutRelationDifferences(memoryModel, baselineModel); //komplexe Teile
        memoryModel.configureAll(config);
        baselineModel.configureAll(config); // Configure after cutting!

        performStaticProgramAnalyses(task, analysisContext, config);
        Context baselineContext = Context.createCopyFrom(analysisContext);
        performStaticWmmAnalyses(task, analysisContext, config);
        performStaticWmmAnalyses(baselineTask, baselineContext, config);

        ProgramEncoder programEncoder = ProgramEncoder.fromConfig(program, analysisContext, config);
        PropertyEncoder propertyEncoder = PropertyEncoder.fromConfig(program, baselineModel, analysisContext, config);
        // We use the original memory model for symmetry breaking because we need axioms
        // to compute the breaking order.
        SymmetryEncoder symmEncoder = SymmetryEncoder.fromConfig(memoryModel, analysisContext, config);
        WmmEncoder baselineEncoder = WmmEncoder.fromConfig(baselineModel, baselineContext, config);
        programEncoder.initializeEncoding(ctx);
        propertyEncoder.initializeEncoding(ctx);
        symmEncoder.initializeEncoding(ctx);
        baselineEncoder.initializeEncoding(ctx);

        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
        //BooleanFormula globalRefinement = bmgr.makeTrue();

        //WMMSolver solver = new WMMSolver(task, analysisContext, cutRelations);
        //Refiner refiner = new Refiner(memoryModel, analysisContext);
        //CAATSolver.Status status = INCONSISTENT;

        BooleanFormula propertyEncoding = propertyEncoder.encodeSpecification(task.getProperty(), ctx);
        if (bmgr.isFalse(propertyEncoding)) {
            logger.info("Verification finished: property trivially holds");
            return PASS;
        }

        FormulaContainer mainFormulaContainer = new FormulaContainer();
        mainFormulaContainer.setFullProgramFormula(programEncoder.encodeFullProgram(ctx));
        mainFormulaContainer.setWmmFormula(baselineEncoder.encodeFullMemoryModel(ctx));
        mainFormulaContainer.setSymmFormula(symmEncoder.encodeFullSymmetry(ctx));

        ThreadPackage mainThreadPackage = new ThreadPackage();
        mainThreadPackage.setSolverContext(ctx);
        mainThreadPackage.setPropertyEncoding(propertyEncoding);
        mainThreadPackage.setProverEnvironment(prover);


        String relationName = RelationNameRepository.RF;
        Relation rf = task.getMemoryModel().getRelationRepository().getRelation(relationName);
        TupleSet rfEncodeSet = rf.getEncodeTupleSet();
        //List<Tuple> tupleList = new ArrayList<>(rfEncodeSet);
        List<Tuple> tupleList = null;// todo hier hier

        Set<Event> branchRepresentatives = analysisContext.get(BranchEquivalence.class).getAllRepresentatives();
        List<Event> eventList = new ArrayList<>(branchRepresentatives);


        FormulaQueueManager fqmgr = new FormulaQueueManager(ctx, task, analysisContext);
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

        int totalThreadnumber = fqmgr.getQueuesize();


        List<Thread> threads = new ArrayList<Thread>(totalThreadnumber);

        resultCollector = new ParallelResultCollector(PASS, 0, maxNumberOfThreads, totalThreadnumber);

        for(int i = 0; i < totalThreadnumber; i++) {
            synchronized (resultCollector){
                while(!resultCollector.canAddThread()){
                    resultCollector.wait();
                }
            }
            int threadID1 = i;
            BooleanFormula threadConstraint = fqmgr.getNextFormula();
            // Case 1: true
            try{
                threads.add(new Thread(()-> {
                    try {
                        runRefinementThread(mainThreadPackage, threadConstraint,
                                threadID1, mainFormulaContainer,
                                analysisContext, cutRelations, propertyEncoder);
                    } catch (InterruptedException e){
                        logger.warn("Timeout elapsed. The SMT solver was stopped");
                        System.out.println("TIMEOUT");
                    } catch (Exception e) {
                        logger.error(e.getMessage());
                        System.out.println("ERROR");
                    }
                }));
                threads.get(threads.size() - 1).start();
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

    public void runRefinementThread(ThreadPackage mainThreadPackage, BooleanFormula myConstraint, int threadID,  FormulaContainer mainFormulaContainer,
                                      Context analysisContext, Set<Relation> cutRelations, PropertyEncoder propertyEncoder)
            throws InterruptedException, SolverException, InvalidConfigurationException {

        ThreadPackage myThreadPackage = new ThreadPackage();
        SolverContext myCTX = SolverContextFactory.createSolverContext(
                Configuration.defaultConfiguration(),
                BasicLogManager.create(Configuration.defaultConfiguration()),
                sdm.getNotifier(),
                SolverContextFactory.Solvers.Z3);

        myThreadPackage.setSolverContext(myCTX);

        BooleanFormula newPropertyEncoding = myCTX.getFormulaManager().translateFrom(mainThreadPackage.getPropertyEncoding(), ctx.getFormulaManager());

        myThreadPackage.setPropertyEncoding(newPropertyEncoding);

        ProverEnvironment myProver = myCTX.newProverEnvironment((SolverContext.ProverOptions.GENERATE_MODELS));

        myThreadPackage.setProverEnvironment(myProver);




        logger.info("Starting encoding using " + myCTX.getVersion());
        myProver.addConstraint(myCTX.getFormulaManager().translateFrom(mainFormulaContainer.getFullProgramFormula(), ctx.getFormulaManager()));
        myProver.addConstraint(myCTX.getFormulaManager().translateFrom(mainFormulaContainer.getWmmFormula(), ctx.getFormulaManager()));
        myProver.addConstraint(myCTX.getFormulaManager().translateFrom(mainFormulaContainer.getSymmFormula(), ctx.getFormulaManager()));

        myProver.push();
        myProver.addConstraint(myThreadPackage.getPropertyEncoding());


        WMMSolver solver = new WMMSolver(task, analysisContext, cutRelations);
        Refiner refiner = new Refiner(task.getMemoryModel(), analysisContext);
        CAATSolver.Status status = INCONSISTENT;

        BooleanFormulaManager bmgr = myCTX.getFormulaManager().getBooleanFormulaManager();
        BooleanFormula globalRefinement = bmgr.makeTrue();

        //  ------ Just for statistics ------
        List<WMMSolver.Statistics> statList = new ArrayList<>();
        int iterationCount = 0;
        long lastTime = System.currentTimeMillis();
        long startTime = lastTime;
        long curTime;
        long totalNativeSolvingTime = 0;
        long totalCaatTime = 0;
        long totalRefiningTime = 0;
        //  ---------------------------------

        logger.info("Refinement procedure started.");
        while (!myProver.isUnsat()) {
        	if(iterationCount == 0 && logger.isDebugEnabled()) {
        		StringBuilder smtStatistics = new StringBuilder("\n ===== SMT Statistics (after first iteration) ===== \n");
        		for(String key : myProver.getStatistics().keySet()) {
        			smtStatistics.append(String.format("\t%s -> %s\n", key, myProver.getStatistics().get(key)));
        		}
        		logger.debug(smtStatistics.toString());
        	}
            iterationCount++;

            curTime = System.currentTimeMillis();
            totalNativeSolvingTime += (curTime - lastTime);

            logger.debug("Solver iteration: \n" +
                            " ===== Iteration: {} =====\n" +
                            "Solving time(ms): {}", iterationCount, curTime - lastTime);

            curTime = System.currentTimeMillis();
            WMMSolver.Result solverResult;

            try (Model model = myProver.getModel()) {
                solverResult = solver.check(model, myCTX);
            } catch (SolverException e) {
                logger.error(e);
                throw e;
            }

            WMMSolver.Statistics stats = solverResult.getStatistics();
            statList.add(stats);
            logger.debug("Refinement iteration:\n{}", stats);

            status = solverResult.getStatus();
            if (status == INCONSISTENT) {
                long refineTime = System.currentTimeMillis();
                DNF<CoreLiteral> reasons = solverResult.getCoreReasons();
                BooleanFormula refinement = refiner.refine(reasons, myCTX);
                myProver.addConstraint(refinement);
                globalRefinement = bmgr.and(globalRefinement, refinement); // Track overall refinement progress
                totalRefiningTime += (System.currentTimeMillis() - refineTime);

                if (REFINEMENT_GENERATE_GRAPHVIZ_DEBUG_FILES) {
                    generateGraphvizFiles(task, solver.getExecution(), iterationCount, reasons);
                }
                if (logger.isTraceEnabled()) {
                    // Some statistics
                    StringBuilder message = new StringBuilder().append("Found inconsistency reasons:");
                    for (Conjunction<CoreLiteral> cube : reasons.getCubes()) {
                        message.append("\n").append(cube);
                    }
                    logger.trace(message);
                }
            } else {
                // No inconsistencies found, we can't refine
                break;
            }
            totalCaatTime += (System.currentTimeMillis() - curTime);
            lastTime = System.currentTimeMillis();
        }
        iterationCount++;
        curTime = System.currentTimeMillis();
        totalNativeSolvingTime += (curTime - lastTime);

        logger.debug("Final solver iteration:\n" +
                        " ===== Final Iteration: {} =====\n" +
                        "Native Solving/Proof time(ms): {}", iterationCount, curTime - lastTime);
		
        if (logger.isInfoEnabled()) {
            String message;
            switch (status) {
                case INCONCLUSIVE:
                    message = "CAAT Solver was inconclusive (bug?).";
                    break;
                case CONSISTENT:
                    message = "Violation verified.";
                    break;
                case INCONSISTENT:
                    message = "Bounded specification proven.";
                    break;
                default:
                    throw new IllegalStateException("Unknown result type returned by CAAT Solver.");
            }
            logger.info(message);
        }

        if (status == INCONCLUSIVE) {
            // CAATSolver got no result (should not be able to happen), so we cannot proceed further.
            synchronized (resultCollector){
                if(resultCollector.getAggregatedResult().equals(FAIL)){
                    return;
                }
                resultCollector.updateResult(UNKNOWN, threadID, startTime);
                resultCollector.notify();

            }
        }

        Result veriResult;
        long boundCheckTime = 0;
        if (myProver.isUnsat()) {
            // ------- CHECK BOUNDS -------
            lastTime = System.currentTimeMillis();
            myProver.pop();
            // Add bound check
            myProver.addConstraint(propertyEncoder.encodeBoundEventExec(myCTX));
            // Add back the constraints found during Refinement
            // TODO: We actually need to perform a second refinement to check for bound reachability
            //  This is needed for the seqlock.c benchmarks!
            myProver.addConstraint(globalRefinement);
            veriResult = !myProver.isUnsat() ? UNKNOWN : PASS;
            boundCheckTime = System.currentTimeMillis() - lastTime;
        } else {
            veriResult = FAIL;
        }

        if (logger.isInfoEnabled()) {
            logger.info(generateSummary(statList, iterationCount, totalNativeSolvingTime,
                    totalCaatTime, totalRefiningTime, boundCheckTime));
        }

        if(logger.isDebugEnabled()) {        	
            StringBuilder smtStatistics = new StringBuilder("\n ===== SMT Statistics (after final iteration) ===== \n");
    		for(String key : myProver.getStatistics().keySet()) {
    			smtStatistics.append(String.format("\t%s -> %s\n", key, myProver.getStatistics().get(key)));
    		}
    		logger.debug(smtStatistics.toString());
        }

        veriResult = task.getProgram().getAss().getInvert() ? veriResult.invert() : veriResult;
        logger.info("Verification finished with result " + veriResult);
        synchronized (resultCollector){
            if(resultCollector.getAggregatedResult().equals(FAIL)){
                return;
            }
            resultCollector.updateResult(veriResult, threadID, startTime);
            resultCollector.notify();

        }
    }



    // ======================= Helper Methods ======================

    // This method cuts off negated relations that are dependencies of some consistency axiom
    // It ignores dependencies of flagged axioms, as those get eagarly encoded and can be completely
    // ignored for Refinement.
    private static Set<Relation> cutRelationDifferences(Wmm targetWmm, Wmm baselineWmm) {
        // TODO: Add support to move flagged axioms to the baselineWmm
        RelationRepository repo = baselineWmm.getRelationRepository();
        Set<Relation> cutRelations = new HashSet<>();
        Set<Relation> cutCandidates = new HashSet<>();
        targetWmm.getAxioms().stream().filter(ax -> !ax.isFlagged())
                .forEach(ax -> collectDependencies(ax.getRelation(), cutCandidates));
        for (Relation rel : cutCandidates) {
            if (rel instanceof RelMinus) {
                Relation sec = rel.getSecond();
                if (sec.getDependencies().size() != 0 || sec instanceof RelSetIdentity || sec instanceof RelCartesian) {
                    // NOTE: The check for RelSetIdentity/RelCartesian is needed because they appear non-derived
                    // in our Wmm but for CAAT they are derived from unary predicates!
                    logger.info("Found difference {}. Cutting rhs relation {}", rel, sec);
                    cutRelations.add(sec);
                    baselineWmm.addAxiom(new ForceEncodeAxiom(getCopyOfRelation(sec, repo)));
                }
            }
        }
        return cutRelations;
    }

    private static void collectDependencies(Relation root, Set<Relation> collected) {
        if (collected.add(root)) {
            root.getDependencies().forEach(dep -> collectDependencies(dep, collected));
        }
    }

    private static Relation getCopyOfRelation(Relation rel, RelationRepository repo) {
        if (repo.containsRelation(rel.getName())) {
            return repo.getRelation(rel.getName());
        }

        if (rel instanceof RecursiveRelation) {
            throw new IllegalArgumentException(
                    String.format("Cannot cut recursively defined relation %s from memory model. ", rel));
        }

        Relation copy = repo.getRelation(rel.getName());
        if (copy == null) {
            List<Object> deps = new ArrayList<>(rel.getDependencies().size());
            if (rel instanceof RelSetIdentity) {
                deps.add(((RelSetIdentity)rel).getFilter());
            } else if (rel instanceof RelCartesian) {
                deps.add(((RelCartesian) rel).getFirstFilter());
                deps.add(((RelCartesian) rel).getSecondFilter());
            } else if (rel instanceof RelFencerel) {
                deps.add(((RelFencerel)rel).getFenceName());
            } else {
                for (Relation dep : rel.getDependencies()) {
                    deps.add(getCopyOfRelation(dep, repo));
                }
            }

            copy = repo.getRelation(rel.getClass(), deps.toArray());
            if (rel.getIsNamed()) {
                copy.setName(rel.getName());
                repo.updateRelation(copy);
            }
        }

        return copy;
    }

    // -------------------- Printing -----------------------------

    private static CharSequence generateSummary(List<WMMSolver.Statistics> statList, int iterationCount,
                                                long totalNativeSolvingTime, long totalCaatTime,
                                                long totalRefiningTime, long boundCheckTime) {
        long totalModelExtractTime = 0;
        long totalPopulationTime = 0;
        long totalConsistencyCheckTime = 0;
        long totalReasonComputationTime = 0;
        long totalNumReasons = 0;
        long totalNumReducedReasons = 0;
        long totalModelSize = 0;
        long minModelSize = Long.MAX_VALUE;
        long maxModelSize = Long.MIN_VALUE;

        for (WMMSolver.Statistics stats : statList) {
            totalModelExtractTime += stats.getModelExtractionTime();
            totalPopulationTime += stats.getPopulationTime();
            totalConsistencyCheckTime += stats.getConsistencyCheckTime();
            totalReasonComputationTime += stats.getBaseReasonComputationTime() + stats.getCoreReasonComputationTime();
            totalNumReasons += stats.getNumComputedCoreReasons();
            totalNumReducedReasons += stats.getNumComputedReducedCoreReasons();

            totalModelSize += stats.getModelSize();
            minModelSize = Math.min(stats.getModelSize(), minModelSize);
            maxModelSize = Math.max(stats.getModelSize(), maxModelSize);
        }

        StringBuilder message = new StringBuilder().append("Summary").append("\n")
                .append(" ======== Summary ========").append("\n")
                .append("Number of iterations: ").append(iterationCount).append("\n")
                .append("Total native solving time(ms): ").append(totalNativeSolvingTime + boundCheckTime).append("\n")
                .append("   -- Bound check time(ms): ").append(boundCheckTime).append("\n")
                .append("Total CAAT solving time(ms): ").append(totalCaatTime).append("\n")
                .append("   -- Model extraction time(ms): ").append(totalModelExtractTime).append("\n")
                .append("   -- Population time(ms): ").append(totalPopulationTime).append("\n")
                .append("   -- Consistency check time(ms): ").append(totalConsistencyCheckTime).append("\n")
                .append("   -- Reason computation time(ms): ").append(totalReasonComputationTime).append("\n")
                .append("   -- Refining time(ms): ").append(totalRefiningTime).append("\n")
                .append("   -- #Computed core reasons: ").append(totalNumReasons).append("\n")
                .append("   -- #Computed core reduced reasons: ").append(totalNumReducedReasons).append("\n");
        if (statList.size() > 0) {
            message.append("   -- Min model size (#events): ").append(minModelSize).append("\n")
                    .append("   -- Average model size (#events): ").append(totalModelSize / statList.size()).append("\n")
                    .append("   -- Max model size (#events): ").append(maxModelSize).append("\n");
        }

        return message;
    }

    // This code is pure debugging code that will generate graphical representations
    // of each refinement iteration.
    // Generate .dot files and .png files per iteration
    private static void generateGraphvizFiles(VerificationTask task, ExecutionModel model, int iterationCount, DNF<CoreLiteral> reasons) {
        //   =============== Visualization code ==================
        // The edgeFilter filters those co/rf that belong to some violation reason
        BiPredicate<EventData, EventData> edgeFilter = (e1, e2) -> {
            for (Conjunction<CoreLiteral> cube : reasons.getCubes()) {
                for (CoreLiteral lit : cube.getLiterals()) {
                    if (lit instanceof RelLiteral) {
                        RelLiteral edgeLit = (RelLiteral) lit;
                        if (model.getData(edgeLit.getData().getFirst()).get() == e1 &&
                                model.getData(edgeLit.getData().getSecond()).get() == e2) {
                            return true;
                        }
                    }
                }
            }
            return false;
        };

        String programName = task.getProgram().getName();
        programName = programName.substring(0, programName.lastIndexOf("."));
        String directoryName = String.format("%s/refinement/%s-%s-debug/", System.getenv("DAT3M_OUTPUT"), programName, task.getProgram().getArch());
        String fileNameBase = String.format("%s-%d", programName, iterationCount);
        // File with reason edges only
        generateGraphvizFile(model, iterationCount, edgeFilter, directoryName, fileNameBase);
        // File with all edges
        generateGraphvizFile(model, iterationCount, (x,y) -> true, directoryName, fileNameBase + "-full");
    }

    private Wmm createDefaultWmm() {
        Wmm baseline = new Wmm();
        RelationRepository repo = baseline.getRelationRepository();
        Relation rf = repo.getRelation(RF);
        if(baselines.contains(Baseline.UNIPROC)) {
            // ---- acyclic(po-loc | rf) ----
            Relation poloc = repo.getRelation(POLOC);
            Relation co = repo.getRelation(CO);
            Relation fr = repo.getRelation(FR);
            Relation porf = new RelUnion(poloc, rf);
            repo.addRelation(porf);
            Relation porfco = new RelUnion(porf, co);
            repo.addRelation(porfco);
            Relation porfcofr = new RelUnion(porfco, fr);
            repo.addRelation(porfcofr);
            baseline.addAxiom(new Acyclic(porfcofr));
        }
        if(baselines.contains(Baseline.NO_OOTA)) {
            // ---- acyclic (dep | rf) ----
            Relation data = repo.getRelation(DATA);
            Relation ctrl = repo.getRelation(CTRL);
            Relation addr = repo.getRelation(ADDR);
            Relation dep = new RelUnion(data, addr);
            repo.addRelation(dep);
            dep = new RelUnion(ctrl, dep);
            repo.addRelation(dep);
            Relation hb = new RelUnion(dep, rf);
            repo.addRelation(hb);
            baseline.addAxiom(new Acyclic(hb));
        }
        if(baselines.contains(Baseline.ATOMIC_RMW)) {
            // ---- empty (rmw & fre;coe) ----
            Relation rmw = repo.getRelation(RMW);
            Relation coe = repo.getRelation(COE);
            Relation fre = repo.getRelation(FRE);
            Relation frecoe = new RelComposition(fre, coe);
            repo.addRelation(frecoe);
            Relation rmwANDfrecoe = new RelIntersection(rmw, frecoe);
            repo.addRelation(rmwANDfrecoe);
            baseline.addAxiom(new Empty(rmwANDfrecoe));
        }
        return baseline;
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
}
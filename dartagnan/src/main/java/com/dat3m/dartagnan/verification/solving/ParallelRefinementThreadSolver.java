package com.dat3m.dartagnan.verification.solving;

import com.dat3m.dartagnan.configuration.Baseline;
import com.dat3m.dartagnan.encoding.*;
import com.dat3m.dartagnan.program.Program;
import com.dat3m.dartagnan.program.analysis.ExecutionAnalysis;
import com.dat3m.dartagnan.program.event.Tag;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.program.filter.FilterAbstract;
import com.dat3m.dartagnan.program.filter.FilterBasic;
import com.dat3m.dartagnan.solver.caat.CAATSolver;
import com.dat3m.dartagnan.solver.caat4wmm.Refiner;
import com.dat3m.dartagnan.solver.caat4wmm.WMMSolver;
import com.dat3m.dartagnan.solver.caat4wmm.coreReasoning.CoreLiteral;
import com.dat3m.dartagnan.solver.caat4wmm.coreReasoning.ExecLiteral;
import com.dat3m.dartagnan.solver.caat4wmm.coreReasoning.RelLiteral;
import com.dat3m.dartagnan.utils.logic.Conjunction;
import com.dat3m.dartagnan.utils.logic.DNF;
import com.dat3m.dartagnan.verification.Context;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.verification.model.EventData;
import com.dat3m.dartagnan.verification.model.ExecutionModel;
import com.dat3m.dartagnan.wmm.Definition;
import com.dat3m.dartagnan.wmm.Relation;
import com.dat3m.dartagnan.wmm.Wmm;
import com.dat3m.dartagnan.wmm.axiom.Acyclic;
import com.dat3m.dartagnan.wmm.axiom.Empty;
import com.dat3m.dartagnan.wmm.axiom.ForceEncodeAxiom;
import com.dat3m.dartagnan.wmm.definition.*;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.java_smt.SolverContextFactory;
import org.sosy_lab.java_smt.api.*;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
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
public class ParallelRefinementThreadSolver extends AbstractParallelThreadSolver {

    private final static Logger logger = LogManager.getLogger(ParallelRefinementThreadSolver.class);


    private final Set<Relation> mainCutRelations;
    private final ParallelRefinementCollector mainRefinementCollector;


    private final int refreshInterval;


    private final ConcurrentLinkedQueue<Conjunction<CoreLiteral>> myReasonsQueue;


    // =========================== Configurables ===========================

    @Option(name=BASELINE,
            description="Refinement starts from this baseline WMM.",
            secure=true,
            toUppercase=true)
    private EnumSet<Baseline> baselines = EnumSet.noneOf(Baseline.class);

    // ======================================================================

    public ParallelRefinementThreadSolver(VerificationTask mainTask, SplittingManager mainSPMGR, ShutdownManager sdm,
                                          ParallelResultCollector mainResultCollector, ParallelRefinementCollector mainRefinementCollector,
                                          SolverContextFactory.Solvers solver, Configuration solverConfig, int threadID,
                                          ParallelSolverConfiguration parallelConfig, Set<Relation> cutRelations, ThreadStatisticManager myStatisticManager)
            throws InvalidConfigurationException{

        super(mainTask, mainSPMGR, sdm, mainResultCollector, solver, solverConfig, threadID, parallelConfig, myStatisticManager);


        this.mainRefinementCollector = mainRefinementCollector;

        this.refreshInterval = parallelConfig.getClauseSharingInterval();
        this.myReasonsQueue = new ConcurrentLinkedQueue<Conjunction<CoreLiteral>>();

        this.mainCutRelations = cutRelations;




    }



    public void run() throws InterruptedException, SolverException, InvalidConfigurationException {

        myStatisticManager.reportStart();
        logger.info("Thread " + myThreadID + ": " + "ThreadSolver Run starts");


        //------------------------
        fetchSplittingObjects();
        mainRefinementCollector.registerReasonQueue(myReasonsQueue);

        //----------------------------------------

        Context baselineContext;
        VerificationTask baselineTask;
        Wmm memoryModel;
        Context analysisContext;


        synchronized (mainTask){
            Program program = mainTask.getProgram();
            memoryModel = mainTask.getMemoryModel();
            Wmm baselineModel = createDefaultWmm();
            analysisContext = Context.create();
            Configuration config = mainTask.getConfig();
            baselineTask = VerificationTask.builder()
                    .withConfig(mainTask.getConfig()).build(program, baselineModel, mainTask.getProperty());

            //preprocessProgram(mainTask, config);
            //preprocessMemoryModel(mainTask);
            // We cut the rhs of differences to get a semi-positive model, if possible.
            // This call modifies the baseline model!
            //cutRelations = cutRelationDifferences(memoryModel, baselineModel); //note nur einmal im main
            //memoryModel.configureAll(config);       nur einmal im main
            //baselineModel.configureAll(config); // Configure after cutting! //nur einmal im main

            switch (mainParallelConfig.getStaticProgramAnalysis()){
                case BASELINE_SPA:
                    performStaticProgramAnalyses(mainTask, analysisContext, config);
                    break;
                case SPA_WITH_ASSUMPTIONS:
                    performAlternativeStaticProgramAnalyses(mainTask, analysisContext, config, trueEventList, falseEventList);
                    break;
                default:
                    throw(new Error(mainParallelConfig.getStaticProgramAnalysis().name() + "is not a supported static analysis of ParallelRefinementThreadSolver."));
            }
            performStaticProgramAnalyses(mainTask, analysisContext, config);
            //performAlternativeStaticProgramAnalyses(mainTask, analysisContext, config, trueEventList, falseEventList);
            baselineContext = Context.createCopyFrom(analysisContext);
            performStaticWmmAnalyses(mainTask, analysisContext, config);
            performStaticWmmAnalyses(baselineTask, baselineContext, config);
        }

        context = EncodingContext.of(baselineTask, baselineContext, myCTX);
        ProgramEncoder programEncoder = ProgramEncoder.withContext(context);
        PropertyEncoder propertyEncoder = PropertyEncoder.withContext(context);
        // We use the original memory model for symmetry breaking because we need axioms
        // to compute the breaking order.
        SymmetryEncoder symmEncoder = SymmetryEncoder.withContext(context, memoryModel, analysisContext);
        WmmEncoder baselineEncoder = WmmEncoder.withContext(context);
        programEncoder.initializeEncoding(myCTX);
        propertyEncoder.initializeEncoding(myCTX);
        symmEncoder.initializeEncoding(myCTX);
        baselineEncoder.initializeEncoding(myCTX);

        BooleanFormulaManager bmgr = myCTX.getFormulaManager().getBooleanFormulaManager();
        BooleanFormula globalRefinement = bmgr.makeTrue();



        WMMSolver solver = WMMSolver.withContext(context, mainCutRelations, mainTask, analysisContext);
        Refiner refiner = new Refiner(analysisContext);
        CAATSolver.Status status = INCONSISTENT;


        BooleanFormula myFormula = generateMyFormula();
        myProver.addConstraint(myFormula);
        //----------------------------------------


        BooleanFormula propertyEncoding = propertyEncoder.encodeSpecification();
        if(bmgr.isFalse(propertyEncoding)) {
            logger.info("Verification finished: property trivially holds");
            res = PASS;
            synchronized(mainResultCollector){
                mainResultCollector.updateResult(res, myThreadID, myStatisticManager);
                mainResultCollector.notify();
            }
            myStatisticManager.reportResult(res);
            mainRefinementCollector.deregisterReasonQueue(myReasonsQueue);
       	    return;
        }

        logger.info("Starting encoding using " + myCTX.getVersion());
        myProver.addConstraint(programEncoder.encodeFullProgram());
        myProver.addConstraint(baselineEncoder.encodeFullMemoryModel());
        myProver.addConstraint(symmEncoder.encodeFullSymmetryBreaking());

        myProver.push();
        myProver.addConstraint(propertyEncoding);




        myStatisticManager.reportPreprocessingTime();

        //  ------ Just for statistics ------
        List<WMMSolver.Statistics> statList = new ArrayList<>();
        int iterationCount = 0;
        long lastTime = System.currentTimeMillis();
        long curTime;
        long totalNativeSolvingTime = 0;
        long totalCaatTime = 0;
        long totalRefiningTime = 0;
        //  ---------------------------------

        //...................................................Refinement..LOOP.....................................
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
            myStatisticManager.addCAATSolverTime(curTime - lastTime);

            logger.debug("Solver iteration: \n" +
                            " ===== Iteration: {} =====\n" +
                            "Solving time(ms): {}", iterationCount, curTime - lastTime);

            curTime = System.currentTimeMillis();
            WMMSolver.Result solverResult;
            try (Model model = myProver.getModel()) {
                solverResult = solver.check(model);
            } catch (SolverException e) {
                logger.error("Thread " + myThreadID + ": " + e);
                throw e;
            }
            myStatisticManager.addWMMSolverTime(System.currentTimeMillis() - curTime);


            if((iterationCount % refreshInterval) - 1 == 0 || refreshInterval == 1){
                long newTime = System.currentTimeMillis();
                addForeignReasons(refiner, analysisContext.get(ExecutionAnalysis.class));
                long tookRTime = System.currentTimeMillis() - newTime;
                myStatisticManager.addClauseReceivingTime(tookRTime);
            }

            WMMSolver.Statistics stats = solverResult.getStatistics();
            statList.add(stats);
            logger.debug("Refinement iteration:\n{}", stats);

            status = solverResult.getStatus();
            if (status == INCONSISTENT) {
                long refineTime = System.currentTimeMillis();
                DNF<CoreLiteral> reasons = solverResult.getCoreReasons();

                long newTime = System.currentTimeMillis();
                mainRefinementCollector.addReason(reasons, myReasonsQueue, myStatisticManager);
                long tookSTime = System.currentTimeMillis() - newTime;
                myStatisticManager.addClauseSharingTime(tookSTime);

                BooleanFormula refinement = refiner.refine(reasons, context);
                myProver.addConstraint(refinement);
                globalRefinement = bmgr.and(globalRefinement, refinement); // Track overall refinement progress
                totalRefiningTime += (System.currentTimeMillis() - refineTime);

                if (REFINEMENT_GENERATE_GRAPHVIZ_DEBUG_FILES) {
                    generateGraphvizFiles(mainTask, solver.getExecution(), iterationCount, reasons);
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
            res = UNKNOWN;
            synchronized(mainResultCollector){
                mainResultCollector.updateResult(res, myThreadID, myStatisticManager);
                mainResultCollector.notify();
            }
            myStatisticManager.reportResult(res);
            mainRefinementCollector.deregisterReasonQueue(myReasonsQueue);
            return;
        }

        long boundCheckTime = 0;
        if (myProver.isUnsat()) {
            // ------- CHECK BOUNDS -------
            lastTime = System.currentTimeMillis();
            myProver.pop();
            // Add bound check
            myProver.addConstraint(propertyEncoder.encodeBoundEventExec());
            // Add back the constraints found during Refinement
            // TODO: We actually need to perform a second refinement to check for bound reachability
            //  This is needed for the seqlock.c benchmarks!
            myProver.addConstraint(globalRefinement);
            res = !myProver.isUnsat() ? UNKNOWN : PASS;
            boundCheckTime = System.currentTimeMillis() - lastTime;
        } else {
            res = FAIL;
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

        synchronized(mainTask){res = mainTask.getProgram().getAss().getInvert() ? res.invert() : res;}

        synchronized(mainResultCollector){
            mainResultCollector.updateResult(res, myThreadID, myStatisticManager);
            mainResultCollector.notify();
        }
        myStatisticManager.reportResult(res);
        mainRefinementCollector.deregisterReasonQueue(myReasonsQueue);


        logger.info("Thread " + myThreadID + ": " + "Verification finished with result " + res);
    }
    // ======================= Helper Methods ======================

    // This method cuts off negated relations that are dependencies of some consistency axiom
    // It ignores dependencies of flagged axioms, as those get eagarly encoded and can be completely
    // ignored for Refinement.
    private static Set<Relation> cutRelationDifferences(Wmm targetWmm, Wmm baselineWmm) {
        // TODO: Add support to move flagged axioms to the baselineWmm
        Set<Relation> cutRelations = new HashSet<>();
        Set<Relation> cutCandidates = new HashSet<>();
        targetWmm.getAxioms().stream().filter(ax -> !ax.isFlagged())
                .forEach(ax -> collectDependencies(ax.getRelation(), cutCandidates));
        for (Relation rel : cutCandidates) {
            if (rel.getDefinition() instanceof Difference) {
                Relation sec = ((Difference) rel.getDefinition()).complement;
                if (!sec.getDependencies().isEmpty() || sec.getDefinition() instanceof Identity || sec.getDefinition() instanceof CartesianProduct) {
                    // NOTE: The check for RelSetIdentity/RelCartesian is needed because they appear non-derived
                    // in our Wmm but for CAAT they are derived from unary predicates!
                    logger.info("Found difference {}. Cutting rhs relation {}", rel, sec);
                    cutRelations.add(sec);
                    baselineWmm.addAxiom(new ForceEncodeAxiom(getCopyOfRelation(sec, baselineWmm)));
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

    private static Relation getCopyOfRelation(Relation rel, Wmm m) {
        Relation namedCopy = m.getRelation(rel.getName());
        if (namedCopy != null) {
            return namedCopy;
        }
        Relation copy = m.newRelation(rel.getName());
        return m.addDefinition(rel.accept(new RelationCopier(m, copy)));
    }

    private static final class RelationCopier implements Definition.Visitor<Definition> {
        final Wmm targetModel;
        final Relation relation;
        RelationCopier(Wmm m, Relation r) {
            targetModel = m;
            relation = r;
        }
        @Override public Definition visitUnion(Relation r, Relation... o) { return new Union(relation, copy(o)); }
        @Override public Definition visitIntersection(Relation r, Relation... o) { return new Intersection(relation, copy(o)); }
        @Override public Definition visitDifference(Relation r, Relation r1, Relation r2) { return new Difference(relation, copy(r1), copy(r2)); }
        @Override public Definition visitComposition(Relation r, Relation r1, Relation r2) { return new Composition(relation, copy(r1), copy(r2)); }
        @Override public Definition visitInverse(Relation r, Relation r1) { return new Inverse(relation, copy(r1)); }
        @Override public Definition visitDomainIdentity(Relation r, Relation r1) { return new DomainIdentity(relation, copy(r1)); }
        @Override public Definition visitRangeIdentity(Relation r, Relation r1) { return new RangeIdentity(relation, copy(r1)); }
        @Override public Definition visitTransitiveClosure(Relation r, Relation r1) { return new TransitiveClosure(relation, copy(r1)); }
        @Override public Definition visitIdentity(Relation r, FilterAbstract filter) { return new Identity(relation, filter); }
        @Override public Definition visitProduct(Relation r, FilterAbstract f1, FilterAbstract f2) { return new CartesianProduct(relation, f1, f2); }
        @Override public Definition visitFences(Relation r, FilterAbstract type) { return new Fences(relation, type); }
        private Relation copy(Relation r) { return getCopyOfRelation(r, targetModel); }
        private Relation[] copy(Relation[] r) {
            Relation[] a = new Relation[r.length];
            for (int i = 0; i < r.length; i++) {
                a[i] = copy(r[i]);
            }
            return a;
        }
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
        Relation rf = baseline.getRelation(RF);
        if(baselines.contains(Baseline.UNIPROC)) {
            // ---- acyclic(po-loc | rf) ----
            Relation poloc = baseline.getRelation(POLOC);
            Relation co = baseline.getRelation(CO);
            Relation fr = baseline.getRelation(FR);
            Relation porf = baseline.addDefinition(new Union(baseline.newRelation(), poloc, rf));
            Relation porfco = baseline.addDefinition(new Union(baseline.newRelation(), porf, co));
            Relation porfcofr = baseline.addDefinition(new Union(baseline.newRelation(), porfco, fr));
            baseline.addAxiom(new Acyclic(porfcofr));
        }
        if(baselines.contains(Baseline.NO_OOTA)) {
            // ---- acyclic (dep | rf) ----
            Relation data = baseline.getRelation(DATA);
            Relation ctrl = baseline.getRelation(CTRL);
            Relation addr = baseline.getRelation(ADDR);
            Relation dep = baseline.addDefinition(new Union(baseline.newRelation(), data, addr));
            Relation dep2 = baseline.addDefinition(new Union(baseline.newRelation(), ctrl, dep));
            Relation hb = baseline.addDefinition(new Union(baseline.newRelation(), dep2, rf));
            baseline.addAxiom(new Acyclic(hb));
        }
        if(baselines.contains(Baseline.ATOMIC_RMW)) {
            // ---- empty (rmw & fre;coe) ----
            Relation rmw = baseline.getRelation(RMW);
            Relation coe = baseline.getRelation(COE);
            Relation fre = baseline.getRelation(FRE);
            Relation frecoe = baseline.addDefinition(new Composition(baseline.newRelation(), fre, coe));
            Relation rmwANDfrecoe = baseline.addDefinition(new Intersection(baseline.newRelation(), rmw, frecoe));
            baseline.addAxiom(new Empty(rmwANDfrecoe));
        }
        return baseline;
    }



    private void addForeignReasons(Refiner refiner, ExecutionAnalysis exec)
            throws InterruptedException{
        long timeBefore = System.currentTimeMillis();
        int filterCount = 0;
        if(myReasonsQueue.isEmpty()){return;}
        Conjunction<CoreLiteral> reason = myReasonsQueue.poll();
        while (reason != null) {
            switch (mainParallelConfig.getClauseReceivingFilter()) {
                case NO_CR_FILTER:
                    myProver.addConstraint(refiner.refineConjunction(reason, context));
                    break;
                case IMPLIES_CR_FILTER:
                    if(imp_CR_filter(exec, reason)){
                        myProver.addConstraint(refiner.refineConjunction(reason, context));
                    }else{
                        filterCount++;
                    }
                    break;
                case IMP_AND_ME_CR_FILTER:
                    if(imp_me_CR_filter(exec, reason)){
                        myProver.addConstraint(refiner.refineConjunction(reason, context));
                    }else{
                        filterCount++;
                    }
                    break;
                case MUTUALLY_EXCLUSIVE_CR_FILTER:
                    if(me_CR_filter(exec, reason)){
                        myProver.addConstraint(refiner.refineConjunction(reason, context));
                    }else{
                        filterCount++;
                    }
                    break;
                default:
                    throw (new Error("unreachable code reached. not Implemented CR Filter."));
            }
            reason = myReasonsQueue.poll();
        }
        long timeAfter = System.currentTimeMillis();
        myStatisticManager.addClauseReceivingFilterTime(timeAfter - timeBefore);
        myStatisticManager.clauseReceivingCountInc(filterCount);
        //logger.info("Thread " + myThreadID + ": " + total + " reasons");
        //logger.info("Thread " + myThreadID + ": " + added + " added reasons");
        //logger.info("Thread " + myThreadID + ": " + (total - added) + " filtered reasons");
        //long tookTime = System.currentTimeMillis()-timenow;
        //logger.info("Thread " + myThreadID + ": " + tookTime + " tooktime");
        //totalTime += tookTime;
        //logger.info("Thread " + myThreadID + ": " + totalTime + " totaltooktime");
    }
    private Boolean imp_me_CR_filter(ExecutionAnalysis exec, Conjunction<CoreLiteral> reason){

        Set<Event> reasonEvents = new HashSet<Event>();
        for (CoreLiteral lit : reason.getLiterals()){
            if (lit instanceof ExecLiteral){
                reasonEvents.add(((ExecLiteral) lit).getData());
            }
            if(lit instanceof RelLiteral){
                reasonEvents.add(((RelLiteral) lit).getData().getFirst());
                reasonEvents.add(((RelLiteral) lit).getData().getSecond());
            }
        }

        for(Event reasonEvent:reasonEvents){
            reasonEvent.getThread().getCache().getEvents(FilterBasic.get(Tag.VISIBLE));
            for(Event trueEvent : trueEventList){
                if(exec.areMutuallyExclusive(reasonEvent,trueEvent)){
                    return false;
                }
            }
            for(Event falseEvent : falseEventList){
                if(exec.isImplied(reasonEvent, falseEvent)){
                    return false;
                }
            }
        }

        for(Event reasonEvent:reasonEvents){
            for (Tuple trueTuple:trueTupleList){
                if(exec.areMutuallyExclusive(reasonEvent, trueTuple.getFirst()) || exec.areMutuallyExclusive(reasonEvent, trueTuple.getSecond())){
                    return false;
                }
            }
        }

        return true;
    }

    private Boolean me_CR_filter(ExecutionAnalysis exec, Conjunction<CoreLiteral> reason){
        Set<Event> reasonEvents = new HashSet<Event>();
        for (CoreLiteral lit : reason.getLiterals()){
            if (lit instanceof ExecLiteral){
                reasonEvents.add(((ExecLiteral) lit).getData());
            }
            if(lit instanceof RelLiteral){
                reasonEvents.add(((RelLiteral) lit).getData().getFirst());
                reasonEvents.add(((RelLiteral) lit).getData().getSecond());
            }
        }

        for(Event reasonEvent:reasonEvents){
            for(Event trueEvent : trueEventList){
                if(exec.areMutuallyExclusive(reasonEvent,trueEvent)){
                    return false;
                }
            }
        }

        for(Event reasonEvent:reasonEvents){
            for (Tuple trueTuple:trueTupleList){
                if(exec.areMutuallyExclusive(reasonEvent, trueTuple.getFirst()) || exec.areMutuallyExclusive(reasonEvent, trueTuple.getSecond())){
                    return false;
                }
            }
        }

        return true;
    }

    private Boolean imp_CR_filter(ExecutionAnalysis exec, Conjunction<CoreLiteral> reason){
        Set<Event> reasonEvents = new HashSet<Event>();
        for (CoreLiteral lit : reason.getLiterals()){
            if (lit instanceof ExecLiteral){
                reasonEvents.add(((ExecLiteral) lit).getData());
            }
            if(lit instanceof RelLiteral){
                reasonEvents.add(((RelLiteral) lit).getData().getFirst());
                reasonEvents.add(((RelLiteral) lit).getData().getSecond());
            }
        }

        for(Event reasonEvent:reasonEvents){
            for(Event falseEvent : falseEventList){
                if(exec.isImplied(reasonEvent, falseEvent)){
                    return false;
                }
            }
        }
        return true;
    }




    /*private BooleanFormula generateTupleFormula(){
        BooleanFormulaManager bmgr = myCTX.getFormulaManager().getBooleanFormulaManager();
        BooleanFormula myFormula = bmgr.makeTrue();
        for (Tuple trueTuple : trueTupleList){
            BooleanFormula var = mainTask.getMemoryModel().getRelation(mainSPMGR.getRelationName()).getSMTVar(trueTuple, context);
            myFormula = bmgr.and(myFormula, var);
        }
        for (Tuple falseTuple : falseTupleList){
            BooleanFormula notVar = bmgr.not(mainTask.getMemoryModel().getRelation(mainSPMGR.getRelationName()).getSMTVar(falseTuple, context));
            myFormula = bmgr.and(myFormula, notVar);
        }
        logger.info("Thread " + myThreadID + ": generated Formula " + myFormula);
        return myFormula;


    }*/

    /*private BooleanFormula generateEventFormula(){
        BooleanFormulaManager bmgr = myCTX.getFormulaManager().getBooleanFormulaManager();
        BooleanFormula myFormula = bmgr.makeTrue();
        for (Event trueEvent : trueEventList){
            BooleanFormula var = context.execution(trueEvent);
            myFormula = bmgr.and(myFormula, var);
        }
        for (Event falseEvent : falseEventList){
            BooleanFormula notVar = bmgr.not(context.execution(falseEvent));
            myFormula = bmgr.and(myFormula, notVar);
        }
        logger.info("Thread " + myThreadID + ": generated Formula " + myFormula);
        return myFormula;


    }*/
}

 /*   //------------myformula-Generation------------
    QueueType queueType = mainFQMGR.getQueueType();
    BooleanFormula myFormula  = myCTX.getFormulaManager().getBooleanFormulaManager().makeTrue();
        switch (queueType){
                case RELATIONS_SORT:
                case RELATIONS_SHUFFLE:
                case SINGLE_LITERAL:

                case MUTUALLY_EXCLUSIVE_SORT:
                case MUTUALLY_EXCLUSIVE_SHUFFLE:
                case EMPTY:
                myFormula = mainFQMGR.generateRelationFormula(myCTX, context, mainTask, myThreadID);
                break;
                case EVENTS:
                case MUTUALLY_EXCLUSIVE_EVENTS:
                myFormula = mainFQMGR.generateEventFormula(myCTX, context, myThreadID);

                }
                myProver.addConstraint(myFormula);
//----------------------------------------*/
package com.dat3m.dartagnan.verification.solving;

import com.dat3m.dartagnan.configuration.Baseline;
import com.dat3m.dartagnan.encoding.*;
import com.dat3m.dartagnan.program.Program;
import com.dat3m.dartagnan.program.analysis.BranchEquivalence;
import com.dat3m.dartagnan.program.analysis.ExecutionAnalysis;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.program.filter.FilterAbstract;
import com.dat3m.dartagnan.solver.caat4wmm.WMMSolver;
import com.dat3m.dartagnan.solver.caat4wmm.coreReasoning.CoreLiteral;
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
import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;
import com.dat3m.dartagnan.wmm.axiom.Acyclic;
import com.dat3m.dartagnan.wmm.axiom.Empty;
import com.dat3m.dartagnan.wmm.axiom.ForceEncodeAxiom;
import com.dat3m.dartagnan.wmm.definition.*;
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
import org.sosy_lab.java_smt.api.*;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import static com.dat3m.dartagnan.configuration.OptionNames.BASELINE;
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

    private final SolverContext mainCTX;
    private final ProverEnvironment mainProver;
    private final VerificationTask mainTask;

    private Set<Relation> cutRelations;

    private ParallelResultCollector resultCollector;
    private final FormulaQueueManager fqmgr;
    private final ParallelRefinementCollector refinementCollector;
    private final ShutdownManager sdm;
    private final SolverContextFactory.Solvers solverType;
    private final Configuration solverConfig;
    private final ParallelSolverConfiguration parallelConfig;




    // =========================== Configurables ===========================

    @Option(name=BASELINE,
            description="Refinement starts from this baseline WMM.",
            secure=true,
            toUppercase=true)
    private EnumSet<Baseline> baselines = EnumSet.noneOf(Baseline.class);

    // ======================================================================

    private ParallelRefinementSolver(SolverContext c, ProverEnvironment p, VerificationTask t, ShutdownManager sdm, SolverContextFactory.Solvers solverType, Configuration solverConfig, ParallelSolverConfiguration parallelConfig)
            throws InvalidConfigurationException{
        mainCTX = c;
        mainProver = p;
        mainTask = t;
        this.sdm = sdm;
        this.fqmgr = new FormulaQueueManager(parallelConfig);
        this.refinementCollector = new ParallelRefinementCollector(0, parallelConfig);
        this.solverType = solverType;
        this.solverConfig = solverConfig;
        this.parallelConfig = parallelConfig;
    }

    //TODO: We do not yet use Witness information. The problem is that WitnessGraph.encode() generates
    // constraints on hb, which is not encoded in Refinement.
    //TODO (2): Add possibility for Refinement to handle CAT-properties (it ignores them for now).
    public static ParallelRefinementSolver run(SolverContext ctx, ProverEnvironment prover, VerificationTask task, SolverContextFactory.Solvers solverType, Configuration solverConfig,
                                               ShutdownManager sdm, ParallelSolverConfiguration parallelConfig)
            throws InterruptedException, SolverException, InvalidConfigurationException {
        ParallelRefinementSolver s = new ParallelRefinementSolver(ctx, prover, task, sdm, solverType, solverConfig, parallelConfig);
        task.getConfig().inject(s);
        logger.info("{}: {}", BASELINE, s.baselines);
        s.run();
        return s;
    }

    private void run() throws InterruptedException, SolverException, InvalidConfigurationException {


        resultCollector = new ParallelResultCollector(PASS, parallelConfig);

        Program program = mainTask.getProgram();
        Wmm memoryModel = mainTask.getMemoryModel();
        Wmm baselineModel = createDefaultWmm();
        Context analysisContext = Context.create();
        Configuration config = mainTask.getConfig();
        VerificationTask baselineTask = VerificationTask.builder()
                .withConfig(mainTask.getConfig()).build(program, baselineModel, mainTask.getProperty());

        preprocessProgram(mainTask, config);
        preprocessMemoryModel(mainTask);
        // We cut the rhs of differences to get a semi-positive model, if possible.
        // This call modifies the baseline model!
        cutRelations = cutRelationDifferences(memoryModel, baselineModel);
        memoryModel.configureAll(config);
        baselineModel.configureAll(config); // Configure after cutting!

        performStaticProgramAnalyses(mainTask, analysisContext, config);
        Context baselineContext = Context.createCopyFrom(analysisContext);
        performStaticWmmAnalyses(mainTask, analysisContext, config);
        performStaticWmmAnalyses(baselineTask, baselineContext, config);

        context = EncodingContext.of(baselineTask, baselineContext, mainCTX);
        ProgramEncoder programEncoder = ProgramEncoder.withContext(context);
        PropertyEncoder propertyEncoder = PropertyEncoder.withContext(context);
        // We use the original memory model for symmetry breaking because we need axioms
        // to compute the breaking order.
        SymmetryEncoder symmEncoder = SymmetryEncoder.withContext(context, memoryModel, analysisContext);
        WmmEncoder baselineEncoder = WmmEncoder.withContext(context);
        programEncoder.initializeEncoding(mainCTX);
        propertyEncoder.initializeEncoding(mainCTX);
        symmEncoder.initializeEncoding(mainCTX);
        baselineEncoder.initializeEncoding(mainCTX);

        //BooleanFormulaManager bmgr = mainCTX.getFormulaManager().getBooleanFormulaManager();
        //BooleanFormula globalRefinement = bmgr.makeTrue();

        //WMMSolver solver = WMMSolver.withContext(context, cutRelations, mainTask, analysisContext);
        //Refiner refiner = new Refiner(analysisContext);
        //CAATSolver.Status status = INCONSISTENT;

        //BooleanFormula propertyEncoding = propertyEncoder.encodeSpecification();







        int totalThreadnumber = fqmgr.getQueueSize();
        List<Thread> threads = new ArrayList<Thread>(totalThreadnumber);

        //------------------------QueueManager-gets-QObjects----------
        switch (parallelConfig.getFormulaItemType()){
            case CO_RELATION_FORMULAS:
                String relationCOName = RelationNameRepository.CO;
                fqmgr.setRelationName(relationCOName);
                Relation relationCO = baselineTask.getMemoryModel().getRelation(relationCOName);
                RelationAnalysis relationAnalysisCO = context.getAnalysisContext().get(RelationAnalysis.class);
                RelationAnalysis.Knowledge knowledgeCO = relationAnalysisCO.getKnowledge(relationCO);
                TupleSet coEncodeSet = knowledgeCO.getMaySet();
                List<Tuple> tupleListCO = new ArrayList<>(coEncodeSet);
                fqmgr.setTupleList(tupleListCO);
                fqmgr.orderTuples();
                fqmgr.filterTuples(analysisContext);
                break;
            case RF_RELATION_FORMULAS:
                String relationRFName = RelationNameRepository.RF;
                fqmgr.setRelationName(relationRFName);
                Relation relationRF = baselineTask.getMemoryModel().getRelation(relationRFName);
                RelationAnalysis relationAnalysisRF = context.getAnalysisContext().get(RelationAnalysis.class);
                RelationAnalysis.Knowledge knowledge = relationAnalysisRF.getKnowledge(relationRF);
                TupleSet rfEncodeSet = knowledge.getMaySet();
                List<Tuple> tupleListRF = new ArrayList<>(rfEncodeSet);
                fqmgr.setTupleList(tupleListRF);
                fqmgr.orderTuples();
                fqmgr.filterTuples(analysisContext);
                break;
            case EVENT_FORMULAS:
                BranchEquivalence branchEquivalence = context.getAnalysisContext().get(BranchEquivalence.class);
                Set<Event> initialClass = branchEquivalence.getInitialClass();
                List<Event> eventList = branchEquivalence.getAllEquivalenceClasses().stream().filter(c -> c!=initialClass).map(c -> c.getRepresentative()).collect(Collectors.toList());
                fqmgr.setEventList(eventList);
                fqmgr.orderEvents();
                fqmgr.filterEvents(analysisContext);
                break;
            case TAUTOLOGY_FORMULAS:
                break;

            default:
                throw(new InvalidConfigurationException("Formula Type " + parallelConfig.getFormulaQueueStyle().name() +" is not supported in ParallelRefinement."));
        }




        /*switch(queueTypeSetting){
            case EMPTY:
            case SINGLE_LITERAL:
                System.out.println("EMPTY AND SINGLE_LITERAL are not implemented"); //TODO implement :)
            case RELATIONS_SORT:
            case RELATIONS_SHUFFLE:
            case MUTUALLY_EXCLUSIVE_SHUFFLE:
            case MUTUALLY_EXCLUSIVE_SORT:

                String relationName = RelationNameRepository.RF;
                fqmgr.setRelationName(relationName);
                Relation relation = baselineTask.getMemoryModel().getRelation(relationName);
                RelationAnalysis relationAnalysis = context.getAnalysisContext().get(RelationAnalysis.class);
                RelationAnalysis.Knowledge knowledge = relationAnalysis.getKnowledge(relation);
                TupleSet rfEncodeSet = knowledge.getMaySet();
                List<Tuple> tupleList = new ArrayList<>(rfEncodeSet);
                fqmgr.setTupleList(tupleList);
                break;

            case EVENTS:
            case MUTUALLY_EXCLUSIVE_EVENTS:

                Set<Event> branchRepresentatives = context.getAnalysisContext().get(BranchEquivalence.class).getAllRepresentatives();
                List<Event> eventList = new ArrayList<>(branchRepresentatives);
                fqmgr.setEventList(eventList);
                break;
            case EVENTS_SHUFFLE:
                BranchEquivalence branchEquivalence = context.getAnalysisContext().get(BranchEquivalence.class);
                Set<Event> initialClass = branchEquivalence.getInitialClass();
                List<Event> eventListShuffle = branchEquivalence.getAllEquivalenceClasses().stream().filter(c -> c!=initialClass).map(c -> c.getRepresentative()).collect(Collectors.toList());
                Random newRandom = new Random();
                int randomInt = (int)Math.random() * 100000000;
                logger.info("Randomseed: " + randomInt);
                //eventListShuffle.shuffle(randomInt);
                fqmgr.setEventList(eventListShuffle);

        }*/


        logger.info("Starting Thread creation.");

        for(int i = 0; i < totalThreadnumber; i++) {
            Thread.sleep(1);
            synchronized (resultCollector){
                while(!resultCollector.canAddThread()){
                    if(resultCollector.getAggregatedResult().equals(FAIL)) {
                        // TODO: kill all threads
                        sdm.requestShutdown("Done");
                        logger.info("Parallel calculations ended. Result: FAIL");
                        resultCollector.printTimes();
                        res = resultCollector.getAggregatedResult();
                        return;
                    }
                    resultCollector.wait();
                }
            }
            int threadID = i;

            // Case 1: true
            try{
                threads.add(new Thread(()-> {
                    try {
                        runThread(threadID);
                    } catch (InterruptedException e){
                        logger.warn("Timeout elapsed. The SMT solver was stopped");
                        System.out.println("TIMEOUT");
                    } catch (Exception e) {
                        logger.error("Thread " + threadID + ": " + e.getMessage());
                        System.out.println("ERROR");
                    }
                }));
                threads.get(threads.size()-1).start();
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
                    res = resultCollector.getAggregatedResult();
                    return;
                } else {

                    if (resultCollector.getNumberOfFinishedThreads() == totalThreadnumber) {//
                        logger.info("Parallel calculations ended. Result: UNKNOWN/PASS");
                        resultCollector.printTimes();
                        res = resultCollector.getAggregatedResult();
                        return;
                    }
                    logger.info("Mainloop: numberOfResults: " + resultCollector.getNumberOfFinishedThreads() + " totalThreadNumber: " + totalThreadnumber);

                    //TODO : check if threads are still alive
                    resultCollector.wait();
                }
            }
        }

    }


    private void runThread(int threadID)
            throws InterruptedException, SolverException, InvalidConfigurationException{
        ParallelRefinementThreadSolver myThreadSolver = new ParallelRefinementThreadSolver(mainTask, fqmgr, sdm, resultCollector, refinementCollector, solverType, solverConfig, threadID, parallelConfig, cutRelations);
        myThreadSolver.run();
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


}

/*Event e;
Conjunction<CoreLiteral> coreReason;
ExecutionAnalysis exec;

boolean isRelevant =
  coreReason.getLiterals().stream()
    .filter(ExecLiteral.class::isInstance)
    .map(literal -> (Event) literal.getData())
    .noneMatch(o -> exec.areMutuallyExclusive(e, o))*/
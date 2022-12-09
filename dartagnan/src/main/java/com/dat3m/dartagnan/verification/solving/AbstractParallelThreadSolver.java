package com.dat3m.dartagnan.verification.solving;

import com.dat3m.dartagnan.configuration.Baseline;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.wmm.utils.Tuple;
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

import static com.dat3m.dartagnan.configuration.OptionNames.BASELINE;

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
public abstract class AbstractParallelThreadSolver extends ModelChecker {

    protected final static Logger logger = LogManager.getLogger(AbstractParallelThreadSolver.class);

    protected final SolverContext myCTX;
    protected final ProverEnvironment myProver;
    protected final ThreadStatisticManager myStatisticManager;
    protected final int myThreadID;

    protected final VerificationTask mainTask;
    protected final ParallelResultCollector mainResultCollector;
    protected final SplittingManager mainSPMGR;
    protected final ParallelSolverConfiguration mainParallelConfig;

    protected final List<Event> trueEventList;
    protected final List<Event> falseEventList;
    protected final List<Tuple> trueTupleList;
    protected final List<Tuple> falseTupleList;


    // =========================== Configurables ===========================

    @Option(name=BASELINE,
            description="Refinement starts from this baseline WMM.",
            secure=true,
            toUppercase=true)
    private EnumSet<Baseline> baselines = EnumSet.noneOf(Baseline.class);

    // ======================================================================

    public AbstractParallelThreadSolver(VerificationTask mainTask, SplittingManager mainSPMGR, ShutdownManager sdm,
                                        ParallelResultCollector mainResultCollector,
                                        SolverContextFactory.Solvers solver, Configuration solverConfig, int threadID,
                                        ParallelSolverConfiguration parallelConfig, ThreadStatisticManager myStatisticManager)
            throws InvalidConfigurationException{
        myCTX = SolverContextFactory.createSolverContext(
                solverConfig,
                BasicLogManager.create(solverConfig),
                sdm.getNotifier(),
                solver);
        myProver = myCTX.newProverEnvironment(SolverContext.ProverOptions.GENERATE_MODELS);
        this.mainTask = mainTask;
        this.mainSPMGR = mainSPMGR;
        this.mainResultCollector = mainResultCollector;
        this.mainParallelConfig = parallelConfig;
        myThreadID = threadID;
        this.myStatisticManager = myStatisticManager;

        this.trueEventList = new ArrayList<Event>();
        this.falseEventList = new ArrayList<Event>();
        this.trueTupleList = new ArrayList<Tuple>();
        this.falseTupleList = new ArrayList<Tuple>();

    }



    protected abstract void run() throws InterruptedException, SolverException, InvalidConfigurationException;



    protected void fetchSplittingObjects(){
        if (mainParallelConfig.getFormulaGenerator() == ParallelSolverConfiguration.FormulaGenerator.IN_SOLVER) {
            switch (mainParallelConfig.getSplittingObjectType()){
                case CO_RELATION_SPLITTING_OBJECTS:
                case RF_RELATION_SPLITTING_OBJECTS:
                    List<List<Tuple>> tupleListList =  mainSPMGR.generateRelationListPair(myThreadID, myStatisticManager);
                    trueTupleList.addAll(tupleListList.get(0));
                    falseTupleList.addAll(tupleListList.get(1));
                    for(Tuple i : trueTupleList){
                        trueEventList.add(i.getFirst());
                        trueEventList.add(i.getSecond());
                    }

                    break;
                case NO_SPLITTING_OBJECTS:
                    break;
                case BRANCH_EVENTS_SPLITTING_OBJECTS:
                case ALL_EVENTS_SPLITTING_OBJECTS:
                    List<List<Event>> eventListList =  mainSPMGR.generateEventListPair(myThreadID, myStatisticManager);
                    trueEventList.addAll(eventListList.get(0));
                    falseEventList.addAll(eventListList.get(1));
                    break;
                default:
                    throw(new Error("FormulaItemType " + mainParallelConfig.getSplittingObjectType().name() + " is not a supported by fetchSplittingObjects in ParallelThreadSolver."));
            }
        }
    }

    protected BooleanFormula generateMyFormula(){
        BooleanFormula myFormula;
        switch(mainParallelConfig.getFormulaGenerator()){
            case IN_SOLVER:
                switch (mainParallelConfig.getSplittingObjectType()){
                    case RF_RELATION_SPLITTING_OBJECTS:
                    case CO_RELATION_SPLITTING_OBJECTS:
                        myFormula = generateTupleFormula();
                        break;
                    case BRANCH_EVENTS_SPLITTING_OBJECTS:
                    case ALL_EVENTS_SPLITTING_OBJECTS:
                        myFormula = generateEventFormula();
                        break;
                    case NO_SPLITTING_OBJECTS:
                        throw(new Error("Unreachable Code reached. TAUTOLOGY_FORMULAS can only be combined with IN_MANAGER. Check ParallelSolverConfiguration Constructor."));

                    default:
                        throw(new Error(mainParallelConfig.getSplittingObjectType() + "is not supported in generateMyFormula in ParallelThreadSolver."));
                }
                break;
            case DEPRECATED:
                switch (mainParallelConfig.getSplittingObjectType()){
                    case RF_RELATION_SPLITTING_OBJECTS:
                    case CO_RELATION_SPLITTING_OBJECTS:
                        myFormula = mainSPMGR.generateRelationFormula(myCTX, context, mainTask, myThreadID, myStatisticManager);
                        break;
                    case NO_SPLITTING_OBJECTS:
                        myFormula = mainSPMGR.generateEmptyFormula(myCTX, myThreadID);
                        break;
                    case BRANCH_EVENTS_SPLITTING_OBJECTS:
                    case ALL_EVENTS_SPLITTING_OBJECTS:
                        myFormula = mainSPMGR.generateEventFormula(myCTX, context, myThreadID, myStatisticManager);
                        break;
                    default:
                        throw(new Error(mainParallelConfig.getSplittingObjectType() + "is not supported in generateMyFormula in ParallelThreadSolver."));
                }

                break;
            default:
                throw(new Error(mainParallelConfig.getFormulaGenerator().name() + " is not supported in generateMyFormula in ParallelThreadSolver."));

        }
        myStatisticManager.setMyFormula(myFormula);
        return myFormula;
    }

    private BooleanFormula generateTupleFormula(){
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


    }

    private BooleanFormula generateEventFormula(){
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


    }
}

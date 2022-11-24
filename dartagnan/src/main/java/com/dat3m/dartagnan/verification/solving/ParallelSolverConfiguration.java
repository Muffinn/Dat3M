package com.dat3m.dartagnan.verification.solving;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;

import java.util.Random;

@Options
public class ParallelSolverConfiguration {

    @Option(description = "The type of Filter used in the Formulas", name = "formulaQueueStyle", secure = true)
    private SplittingStyle splittingStyle;

    @Option(description = "The type of literal used in the Formulas", name = "formulaItemType", secure = true)
    private SplittingObjectType splittingObjectType;

    @Option(description = "The type of Filter used in the Formulas", name = "formulaItemFilter", secure = true)
    private SplittingObjectFilter splittingObjectFilter;

    @Option(description = "The type of Filter used in the Formulas", name = "formulaItemOrder", secure = true)
    private SplittingObjectSelection splittingObjectSelection;

    @Option(description = "The type of Filter used in the Formulas", name = "formulaGeneration", secure = true)
    private FormulaGenerator formulaGenerator;

    @Option(description = "The type of Filter used in the Formulas", name = "clauseSharingFilter", secure = true)
    private ClauseSharingFilter clauseSharingFilter;

    @Option(description = "The type of Filter used in the Formulas", name = "clauseReceivingFilter", secure = true)
    private ClauseReceivingFilter clauseReceivingFilter;

    @Option(description = "The type of Filter used in the Formulas", name = "queueSettingInt1", secure = true)
    private int queueSettingInt1;

    @Option(description = "The type of Filter used in the Formulas", name = "queueSettingInt2", secure = true)
    private int queueSettingInt2;

    @Option(description = "The type of Filter used in the Formulas", name = "maxConcurrentThreads", secure = true)
    private int maxNumberOfConcurrentThreads;

    private final int numberOfSplits;
    private final int formulaLength;

    @Option(description = "The type of Filter used in the Formulas", name = "randomSeed", secure = true)
    private long randomSeed;

    private final Random shuffleRandom;

    public enum SplittingStyle {
        LINEAR_AND_BINARY_SPLITTING_STYLE, BINARY_SPLITTING_STYLE, LINEAR_SPLITTING_STYLE, NO_SPLITTING_STYLE;
    }

    public enum SplittingObjectType {
        RF_RELATION_SPLITTING_OBJECTS, CO_RELATION_SPLITTING_OBJECTS, EVENT_SPLITTING_OBJECTS, NO_SPLITTING_OBJECTS;
    }

    public enum SplittingObjectFilter {
        NO_SO_FILTER, MUTUALLY_EXCLUSIVE_SO_FILTER, IMPLIES_SO_FILTER, IMP_AND_ME_SO_FILTER;
    }

    public enum SplittingObjectSelection {
        RANDOM_SELECTION, SEEDED_RANDOM_SELECTION, NO_SELECTION, INDEX_SELECTION;
    }


    public enum FormulaGenerator {
        IN_MANAGER, IN_SOLVER;
    }

    public enum ClauseSharingFilter {
        NO_CS_FILTER, DUPLICATE_CS_FILTER, IMPLIED_CS_FILTER, NO_CLAUSE_SHARING;
    }

    public enum ClauseReceivingFilter{
        NO_CR_FILTER, MUTUALLY_EXCLUSIVE_CR_FILTER, IMPLIES_CR_FILTER, IMP_AND_ME_CR_FILTER;
    }

    /**
     * Constructor for ParallelSolverConfig
     * @param splittingObjectType Type of Items in the formulas. Choice of RF-Relation, CO-Relations and Events
     * @param splittingObjectFilter Filter Mutually Exclusive and/or Implied Items
     * @param splittingObjectSelection Order Items random, by ID or not at all
     * @param splittingStyle Tautology or Splits
     * @param formulaGenerator generate Formula in Solver or in the FormulaQueueManager
     * @param clauseSharingFilter filter which clauses are filtered
     * @param queueSettingInt1 Int value used to generate the formulas
     * @param queueSettingInt2 Int value used to generate the formulas
     * @param maxNumberOfConcurrentThreads amount of threads that can run concurrent
     * @throws InvalidConfigurationException some configurations are not supported in combination with other configuration
     */

    public ParallelSolverConfiguration(SplittingStyle splittingStyle, SplittingObjectType splittingObjectType, SplittingObjectFilter splittingObjectFilter,
                                       SplittingObjectSelection splittingObjectSelection, FormulaGenerator formulaGenerator, ClauseSharingFilter clauseSharingFilter,
                                       ClauseReceivingFilter clauseReceivingFilter,
                                       int queueSettingInt1, int queueSettingInt2, int maxNumberOfConcurrentThreads)
            throws InvalidConfigurationException {

        if (splittingObjectType != SplittingObjectType.NO_SPLITTING_OBJECTS) {
            if(splittingStyle == SplittingStyle.NO_SPLITTING_STYLE){
                throw (new InvalidConfigurationException("TAUTOLOGY_FORMULA_STYLE FormulaQueueStyle is only supported with FormulaItemType TAUTOLOGY_FORMULAS."));
            }
            this.splittingObjectType = splittingObjectType;
            this.splittingObjectFilter = splittingObjectFilter;
            this.splittingObjectSelection = splittingObjectSelection;
            this.splittingStyle = splittingStyle;
            this.formulaGenerator = formulaGenerator;
        } else {
            this.splittingObjectType = SplittingObjectType.NO_SPLITTING_OBJECTS;
            this.splittingObjectFilter = SplittingObjectFilter.NO_SO_FILTER;
            this.splittingObjectSelection = SplittingObjectSelection.NO_SELECTION;
            this.splittingStyle = SplittingStyle.NO_SPLITTING_STYLE;
            this.formulaGenerator = FormulaGenerator.IN_MANAGER;
        }
        this.clauseSharingFilter = clauseSharingFilter;
        this.clauseReceivingFilter = clauseReceivingFilter;
        this.queueSettingInt1 =queueSettingInt1;
        this.queueSettingInt2 =queueSettingInt2;
        this.maxNumberOfConcurrentThreads = maxNumberOfConcurrentThreads;
        this.numberOfSplits = calculateNrOfSplits();
        this.formulaLength = calculateFormulaLength();

        if(splittingObjectSelection == SplittingObjectSelection.SEEDED_RANDOM_SELECTION){
            randomSeed = 1337;
        } else {
            randomSeed = new Random().nextLong();
        }
        this.shuffleRandom = new Random(randomSeed);
    }


    /**
     * Constructor for ParallelSolverConfiguration with a chosen Randomseed
     * @param splittingObjectType Type of Items in the formulas. Choice of RF-Relation, CO-Relations and Events
     * @param splittingObjectFilter Filter Mutually Exclusive and/or Implied Items
     * @param splittingObjectSelection Order Items random, by ID or not at all
     * @param splittingStyle Tautology or Splits
     * @param formulaGenerator generate Formula in Solver or in the FormulaQueueManager
     * @param clauseSharingFilter filter which clauses are filtered
     * @param queueSettingInt1 Int value used to generate the formulas
     * @param queueSettingInt2 Int value used to generate the formulas
     * @param maxNumberOfConcurrentThreads amount of threads that can run concurrent
     * @param randomSeed fixed random seed. used if SEEDED_RANDOM_ORDER is enabled
     * @throws InvalidConfigurationException some configurations are not supported in combination with other configuration
     */
    public ParallelSolverConfiguration(SplittingStyle splittingStyle, SplittingObjectType splittingObjectType, SplittingObjectFilter splittingObjectFilter,
                                       SplittingObjectSelection splittingObjectSelection, FormulaGenerator formulaGenerator, ClauseSharingFilter clauseSharingFilter,
                                       ClauseReceivingFilter clauseReceivingFilter,
                                       int queueSettingInt1, int queueSettingInt2, int maxNumberOfConcurrentThreads, long randomSeed)
            throws InvalidConfigurationException {

        if (splittingObjectType != SplittingObjectType.NO_SPLITTING_OBJECTS) {
            if(splittingStyle == SplittingStyle.NO_SPLITTING_STYLE){
                throw (new InvalidConfigurationException("TAUTOLOGY_FORMULA_STYLE FormulaQueueStyle is only supported with FormulaItemType TAUTOLOGY_FORMULAS."));
            }
            this.splittingObjectType = splittingObjectType;
            this.splittingObjectFilter = splittingObjectFilter;
            this.splittingObjectSelection = splittingObjectSelection;
            this.splittingStyle = splittingStyle;
            this.formulaGenerator = formulaGenerator;
        } else {
            this.splittingObjectType = SplittingObjectType.NO_SPLITTING_OBJECTS;
            this.splittingObjectFilter = SplittingObjectFilter.NO_SO_FILTER;
            this.splittingObjectSelection = SplittingObjectSelection.NO_SELECTION;
            this.splittingStyle = SplittingStyle.NO_SPLITTING_STYLE;
            this.formulaGenerator = FormulaGenerator.IN_MANAGER;
        }
        this.clauseSharingFilter = clauseSharingFilter;
        this.clauseReceivingFilter = clauseReceivingFilter;
        this.queueSettingInt1 =queueSettingInt1;
        this.queueSettingInt2 =queueSettingInt2;
        this.maxNumberOfConcurrentThreads = maxNumberOfConcurrentThreads;
        this.numberOfSplits = calculateNrOfSplits();
        this.formulaLength = calculateFormulaLength();

        if(splittingObjectSelection == SplittingObjectSelection.SEEDED_RANDOM_SELECTION){
            this.randomSeed = randomSeed;
        } else {
            this.randomSeed = new Random().nextLong();
        }
        this.shuffleRandom = new Random(this.randomSeed);
    }

    public static ParallelSolverConfiguration defaultConfiguration()
            throws InvalidConfigurationException{
        return  (new ParallelSolverConfiguration(
                SplittingStyle.LINEAR_AND_BINARY_SPLITTING_STYLE,
                SplittingObjectType.EVENT_SPLITTING_OBJECTS,
                SplittingObjectFilter.NO_SO_FILTER,
                SplittingObjectSelection.SEEDED_RANDOM_SELECTION,
                FormulaGenerator.IN_SOLVER,
                ClauseSharingFilter.NO_CS_FILTER,
                ClauseReceivingFilter.NO_CR_FILTER,
                2,
                2,
                4,
                -861449674903621944L
                ));
    }

    private int calculateNrOfSplits() throws InvalidConfigurationException{
        switch(this.splittingStyle){
            case NO_SPLITTING_STYLE:
            case LINEAR_SPLITTING_STYLE:
                return queueSettingInt1;
            case LINEAR_AND_BINARY_SPLITTING_STYLE:
                return queueSettingInt1 * (int) Math.pow(2, queueSettingInt2);

            default:
                throw (new InvalidConfigurationException("Formula QueueStyle not supported by ParallelSolverConfiguration Constructor. Can't calculate number of Split."));
        }
    }

    private int calculateFormulaLength() throws InvalidConfigurationException{
        switch(this.splittingStyle){
            case NO_SPLITTING_STYLE:
                return 0;
            case LINEAR_SPLITTING_STYLE:
                return (queueSettingInt1 - 1);
            case LINEAR_AND_BINARY_SPLITTING_STYLE:
                return (queueSettingInt1 - 1 + queueSettingInt2);
            default:
                throw (new InvalidConfigurationException("Formula QueueStyle not supported by ParallelSolverConfiguration Constructor. Can't Formula Length."));
        }
    }

    //----------------------------------GETTER--------------------------------

    public SplittingObjectType getFormulaItemType() {
        return splittingObjectType;
    }

    public SplittingObjectFilter getFormulaItemFilter() {
        return splittingObjectFilter;
    }

    public SplittingObjectSelection getFormulaItemOrder() {
        return splittingObjectSelection;
    }

    public SplittingStyle getFormulaQueueStyle() {
        return splittingStyle;
    }

    public FormulaGenerator getFormulaGeneration() {
        return formulaGenerator;
    }

    public ClauseSharingFilter getClauseSharingFilter() {
        return clauseSharingFilter;
    }

    public ClauseReceivingFilter getClauseReceivingFilter() {
        return clauseReceivingFilter;
    }

    public int getQueueSettingInt1() {
        return queueSettingInt1;
    }

    public int getQueueSettingInt2() {
        return queueSettingInt2;
    }

    public int getMaxNumberOfConcurrentThreads() {
        return maxNumberOfConcurrentThreads;
    }

    public int getNumberOfSplits() {
        return numberOfSplits;
    }

    public int getFormulaLength() {
        return formulaLength;
    }

    public long getRandomSeed() {
        return randomSeed;
    }

    public Random getShuffleRandom() {
        return shuffleRandom;
    }




}

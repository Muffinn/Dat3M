package com.dat3m.dartagnan.verification.solving;

import org.sosy_lab.common.configuration.InvalidConfigurationException;

import java.util.Random;

public class ParallelSolverConfiguration {

    private final FormulaItemType formulaItemType;
    private final FormulaItemFilter formulaItemFilter;
    private final FormulaItemOrder formulaItemOrder;
    private final FormulaQueueStyle formulaQueueStyle;
    private final FormulaGeneration formulaGeneration;
    private final ClauseSharingFilter clauseSharingFilter;

    private final int queueSettingInt1;
    private final int queueSettingInt2;
    private final int maxNumberOfConcurrentThreads;
    private final int numberOfSplits;
    private final int formulaLength;

    private final long randomSeed;
    private final Random shuffleRandom;

    public enum FormulaItemType {
        RF_RELATION_FORMULAS, CO_RELATION_FORMULAS, EVENT_FORMULAS, TAUTOLOGY_FORMULAS;
    }

    public enum FormulaItemFilter {
        NO_FILTER, MUTUALLY_EXCLUSIVE_FILTER, IMPLIES_FILTER, IMP_AND_ME_FILTER;
    }

    public enum FormulaItemOrder {
        RANDOM_ORDER, SEEDED_RANDOM_ORDER, NO_ORDER, INDEX_ORDER;
    }

    public enum FormulaQueueStyle {
        TREE_SHAPED_FORMULA_QUEUE, TAUTOLOGY_FORMULA_STYLE;
    }

    public enum FormulaGeneration {
        IN_MANAGER, IN_SOLVER;
    }

    public enum ClauseSharingFilter {
        NO_FILTER;
    }

    /**
     * Constructor for ParallelSolverConfig
     * @param formulaItemType Type of Items in the formulas. Choice of RF-Relation, CO-Relations and Events
     * @param formulaItemFilter Filter Mutually Exclusive and/or Implied Items
     * @param formulaItemOrder Order Items random, by ID or not at all
     * @param formulaQueueStyle Tautology or Splits
     * @param formulaGeneration generate Formula in Solver or in the FormulaQueueManager
     * @param clauseSharingFilter filter which clauses are filtered
     * @param queueSettingInt1 Int value used to generate the formulas
     * @param queueSettingInt2 Int value used to generate the formulas
     * @param maxNumberOfConcurrentThreads amount of threads that can run concurrent
     * @throws InvalidConfigurationException some configurations are not supported in combination with other configuration
     */

    public ParallelSolverConfiguration(FormulaItemType formulaItemType, FormulaItemFilter formulaItemFilter, FormulaItemOrder formulaItemOrder,
                                       FormulaQueueStyle formulaQueueStyle, FormulaGeneration formulaGeneration, ClauseSharingFilter clauseSharingFilter,
                                       int queueSettingInt1, int queueSettingInt2, int maxNumberOfConcurrentThreads)
            throws InvalidConfigurationException {

        if (formulaItemType != FormulaItemType.TAUTOLOGY_FORMULAS) {
            if(formulaQueueStyle == FormulaQueueStyle.TAUTOLOGY_FORMULA_STYLE){
                throw (new InvalidConfigurationException("TAUTOLOGY_FORMULA_STYLE FormulaQueueStyle is only supported with FormulaItemType TAUTOLOGY_FORMULAS."));
            }
            this.formulaItemType = formulaItemType;
            this.formulaItemFilter = formulaItemFilter;
            this.formulaItemOrder = formulaItemOrder;
            this.formulaQueueStyle = formulaQueueStyle;
            this.formulaGeneration = formulaGeneration;
        } else {
            this.formulaItemType = FormulaItemType.TAUTOLOGY_FORMULAS;
            this.formulaItemFilter = FormulaItemFilter.NO_FILTER;
            this.formulaItemOrder = FormulaItemOrder.NO_ORDER;
            this.formulaQueueStyle = FormulaQueueStyle.TAUTOLOGY_FORMULA_STYLE;
            this.formulaGeneration = FormulaGeneration.IN_MANAGER;
        }
        this.clauseSharingFilter = clauseSharingFilter;
        this.queueSettingInt1 =queueSettingInt1;
        this.queueSettingInt2 =queueSettingInt2;
        this.maxNumberOfConcurrentThreads = maxNumberOfConcurrentThreads;
        this.numberOfSplits = calculateNrOfSplits();
        this.formulaLength = calculateFormulaLength();

        if(formulaItemOrder == FormulaItemOrder.SEEDED_RANDOM_ORDER){
            randomSeed = 1337;
        } else {
            randomSeed = new Random().nextLong();
        }
        this.shuffleRandom = new Random(randomSeed);
    }


    /**
     * Constructor for ParallelSolverConfiguration with a chosen Randomseed
     * @param formulaItemType Type of Items in the formulas. Choice of RF-Relation, CO-Relations and Events
     * @param formulaItemFilter Filter Mutually Exclusive and/or Implied Items
     * @param formulaItemOrder Order Items random, by ID or not at all
     * @param formulaQueueStyle Tautology or Splits
     * @param formulaGeneration generate Formula in Solver or in the FormulaQueueManager
     * @param clauseSharingFilter filter which clauses are filtered
     * @param queueSettingInt1 Int value used to generate the formulas
     * @param queueSettingInt2 Int value used to generate the formulas
     * @param maxNumberOfConcurrentThreads amount of threads that can run concurrent
     * @param randomSeed fixed random seed. used if SEEDED_RANDOM_ORDER is enabled
     * @throws InvalidConfigurationException some configurations are not supported in combination with other configuration
     */
    public ParallelSolverConfiguration(FormulaItemType formulaItemType, FormulaItemFilter formulaItemFilter, FormulaItemOrder formulaItemOrder,
                                       FormulaQueueStyle formulaQueueStyle, FormulaGeneration formulaGeneration, ClauseSharingFilter clauseSharingFilter,
                                       int queueSettingInt1, int queueSettingInt2, int maxNumberOfConcurrentThreads, long randomSeed)
            throws InvalidConfigurationException {

        if (formulaItemType != FormulaItemType.TAUTOLOGY_FORMULAS) {
            if(formulaQueueStyle == FormulaQueueStyle.TAUTOLOGY_FORMULA_STYLE){
                throw (new InvalidConfigurationException("TAUTOLOGY_FORMULA_STYLE FormulaQueueStyle is only supported with FormulaItemType TAUTOLOGY_FORMULAS."));
            }
            this.formulaItemType = formulaItemType;
            this.formulaItemFilter = formulaItemFilter;
            this.formulaItemOrder = formulaItemOrder;
            this.formulaQueueStyle = formulaQueueStyle;
            this.formulaGeneration = formulaGeneration;
        } else {
            this.formulaItemType = FormulaItemType.TAUTOLOGY_FORMULAS;
            this.formulaItemFilter = FormulaItemFilter.NO_FILTER;
            this.formulaItemOrder = FormulaItemOrder.NO_ORDER;
            this.formulaQueueStyle = FormulaQueueStyle.TAUTOLOGY_FORMULA_STYLE;
            this.formulaGeneration = FormulaGeneration.IN_MANAGER;
        }
        this.clauseSharingFilter = clauseSharingFilter;
        this.queueSettingInt1 =queueSettingInt1;
        this.queueSettingInt2 =queueSettingInt2;
        this.maxNumberOfConcurrentThreads = maxNumberOfConcurrentThreads;
        this.numberOfSplits = calculateNrOfSplits();
        this.formulaLength = calculateFormulaLength();

        if(formulaItemOrder == FormulaItemOrder.SEEDED_RANDOM_ORDER){
            this.randomSeed = randomSeed;
        } else {
            this.randomSeed = new Random().nextLong();
        }
        this.shuffleRandom = new Random(randomSeed);
    }

    public static ParallelSolverConfiguration defaultConfiguration()
            throws InvalidConfigurationException{
        return  (new ParallelSolverConfiguration(
                FormulaItemType.EVENT_FORMULAS,
                FormulaItemFilter.NO_FILTER,
                FormulaItemOrder.NO_ORDER,
                FormulaQueueStyle.TREE_SHAPED_FORMULA_QUEUE,
                FormulaGeneration.IN_SOLVER,
                ClauseSharingFilter.NO_FILTER,
                9,
                0,
                4
                ));
    }

    private int calculateNrOfSplits() throws InvalidConfigurationException{
        switch(this.formulaQueueStyle){
            case TAUTOLOGY_FORMULA_STYLE:
                return queueSettingInt1;
            case TREE_SHAPED_FORMULA_QUEUE:
                return queueSettingInt1 * (int) Math.pow(2, queueSettingInt2);
            default:
                throw (new InvalidConfigurationException("Formula QueueStyle not supported by ParallelSolverConfiguration Constructor. Can't calculate number of Split."));
        }
    }

    private int calculateFormulaLength() throws InvalidConfigurationException{
        switch(this.formulaQueueStyle){
            case TAUTOLOGY_FORMULA_STYLE:
                return 0;
            case TREE_SHAPED_FORMULA_QUEUE:
                return (queueSettingInt1 + queueSettingInt2);
            default:
                throw (new InvalidConfigurationException("Formula QueueStyle not supported by ParallelSolverConfiguration Constructor. Can't Formula Length."));
        }
    }

    //----------------------------------GETTER--------------------------------

    public FormulaItemType getFormulaItemType() {
        return formulaItemType;
    }

    public FormulaItemFilter getFormulaItemFilter() {
        return formulaItemFilter;
    }

    public FormulaItemOrder getFormulaItemOrder() {
        return formulaItemOrder;
    }

    public FormulaQueueStyle getFormulaQueueStyle() {
        return formulaQueueStyle;
    }

    public FormulaGeneration getFormulaGeneration() {
        return formulaGeneration;
    }

    public ClauseSharingFilter getClauseSharingFilter() {
        return clauseSharingFilter;
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

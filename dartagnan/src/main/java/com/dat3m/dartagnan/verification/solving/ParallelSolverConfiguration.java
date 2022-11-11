package com.dat3m.dartagnan.verification.solving;

import org.sosy_lab.common.configuration.InvalidConfigurationException;

public class ParallelSolverConfiguration {

    private final FormulaItemType formulaItemType;
    private final FormulaItemFilter formulaItemFilter;
    private final FormulaItemOrder formulaItemOrder;
    private final FormulaQueueStyle formulaQueueStyle;
    private final FormulaGeneration formulaGeneration;
    private final ClauseSharingFilter clauseSharingFilter;

    private final int queueSettingInt1;
    private final int queueSettingInt2;
    private final int maxNumberOfThreads;

    public enum FormulaItemType {
        RF_RELATION_FORMULAS, CO_RELATION_FORMULAS, EVENT_FORMULAS, TAUTOLOGY_FORMULAS;
    }

    public enum FormulaItemFilter {
        NO_FILTER, MUTUALLY_EXCLUSIVE_FILTER, IMPLIES_FILTER, IMP_AND_ME_FILTER;
    }

    public enum FormulaItemOrder {
        RANDOM_ORDER, NO_ORDER;
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

    public ParallelSolverConfiguration(FormulaItemType formulaItemType, FormulaItemFilter formulaItemFilter, FormulaItemOrder formulaItemOrder,
                                       FormulaQueueStyle formulaQueueStyle, FormulaGeneration formulaGeneration, ClauseSharingFilter clauseSharingFilter,
                                       int queueSettingInt1, int queueSettingInt2, int maxNumberOfThreads)
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
        this.maxNumberOfThreads =maxNumberOfThreads;
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

    public int getMaxNumberOfThreads() {
        return maxNumberOfThreads;
    }
}

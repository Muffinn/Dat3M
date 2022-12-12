package com.dat3m.dartagnan.verification.solving;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;

import java.util.Random;

@Options
public class ParallelSolverConfiguration {

    @Option(description = "The Style of the Splitting: linear, binary or mixed", name = "formulaQueueStyle", secure = true)
    private SplittingStyle splittingStyle;

    @Option(description = "First Int used to generate the splitting", name = "queueSettingInt1", secure = true)
    private int splittingIntN;

    @Option(description = "Second Int used to generate the slitting", name = "queueSettingInt2", secure = true)
    private int splittingIntM;

    private int numberOfSplits; //derived from SplittingStyle, n and m
    private int formulaLength; //derived from SplittingStyle, n and m


    @Option(description = "The maximum number of concurrent threads", name = "maxConcurrentThreads", secure = true)
    private int maxNumberOfConcurrentThreads;

    @Option(description = "The objects used to generate the splitting assumptions", name = "formulaItemType", secure = true)
    private SplittingObjectType splittingObjectType;

    @Option(description = "A filter that filters redundant splitting objects", name = "formulaItemFilter", secure = true)
    private SplittingObjectFilter splittingObjectFilter;

    @Option(description = "The way in which the splitting Objects are selected", name = "formulaItemOrder", secure = true)
    private SplittingObjectSelection splittingObjectSelection;

    @Option(description = "The randomSeed used to chose random Splitting Objects", name = "randomSeed", secure = true)
    private long randomSeed;

    @Option(description = "deprecated only use in_solver", name = "formulaGeneration", secure = true)
    private FormulaGenerator formulaGenerator;

    @Option(description = "Chose if static program analysis should factor in assumptions", name = "staticProgramAnalysis", secure = true)
    private StaticProgramAnalysis staticProgramAnalysis;

    @Option(description = "Chose if shared clauses should be filtered", name = "clauseSharingFilter", secure = true)
    private ClauseSharingFilter clauseSharingFilter;

    @Option(description =  "Set how often Threads collect foreign clauses", name = "clasueSharingInterval", secure = true)
    private int clauseSharingInterval;

    @Option(description = "Chose if received clauses should be filtered", name = "clauseReceivingFilter", secure = true)
    private ClauseReceivingFilter clauseReceivingFilter;






    //data report

    private boolean initialisedFileReport = false;
    private String reportFileName = "noNameInitialized";
    private String architectureName = "noArchInitialized";
    private String targetName = "noTargetNameInitialized";
    private String solverName = "noSovlerNameGiven";



    private int[] chosenEvents;

    private final Random shuffleRandom;

    public enum SplittingStyle {
        LINEAR_AND_BINARY_SPLITTING_STYLE, BINARY_SPLITTING_STYLE, LINEAR_SPLITTING_STYLE, NO_SPLITTING_STYLE;
    }

    public enum SplittingObjectType {
        RF_RELATION_SPLITTING_OBJECTS, CO_RELATION_SPLITTING_OBJECTS, BRANCH_EVENTS_SPLITTING_OBJECTS, ALL_EVENTS_SPLITTING_OBJECTS, NO_SPLITTING_OBJECTS;
    }

    public enum SplittingObjectFilter {
        NO_SO_FILTER, MUTUALLY_EXCLUSIVE_SO_FILTER, IMPLIES_SO_FILTER, IMP_AND_ME_SO_FILTER;
    }

    public enum SplittingObjectSelection {
        RANDOM_SELECTION, SEEDED_RANDOM_SELECTION, NO_SELECTION, INDEX_SELECTION, CHOSEN_SELECTION, SCORE_SELECTION;
    }


    public enum StaticProgramAnalysis {
        BASELINE_SPA, SPA_WITH_ASSUMPTIONS;
    }

    public enum FormulaGenerator {
        DEPRECATED, IN_SOLVER;
    }

    public enum ClauseSharingFilter {
        NO_CS_FILTER, DUPLICATE_CS_FILTER, IMPLIED_CS_FILTER, NO_CLAUSE_SHARING;
    }

    public enum ClauseReceivingFilter{
        NO_CR_FILTER, MUTUALLY_EXCLUSIVE_CR_FILTER, IMPLIES_CR_FILTER, IMP_AND_ME_CR_FILTER;
    }



    /**
     * Constructor for ParallelSolverConfiguration with a chosen Randomseed
     * @param splittingStyle Tautology or Splits
     * @param splittingIntN Int value used to generate the formulas
     * @param splittingIntM Int value used to generate the formulas
     * @param maxNumberOfConcurrentThreads amount of threads that can run concurrent
     * @param splittingObjectType Type of Items in the formulas. Choice of RF-Relation, CO-Relations and Events
     * @param splittingObjectFilter Filter Mutually Exclusive and/or Implied Items
     * @param splittingObjectSelection Order Items random, by ID or not at all
     * @param randomSeed fixed random seed. used if SEEDED_RANDOM_ORDER is enabled
     * @param formulaGenerator generate Formula in Solver or in the FormulaQueueManager
     * @param clauseSharingFilter filter which clauses are filtered
     * @throws InvalidConfigurationException some configurations are not supported in combination with other configuration
     */
    public ParallelSolverConfiguration(SplittingStyle splittingStyle, int splittingIntN, int splittingIntM, int maxNumberOfConcurrentThreads, SplittingObjectType splittingObjectType, SplittingObjectFilter splittingObjectFilter,
                                       SplittingObjectSelection splittingObjectSelection, long randomSeed, StaticProgramAnalysis staticProgramAnalysis,
                                       FormulaGenerator formulaGenerator, ClauseSharingFilter clauseSharingFilter, int clauseSharingInterval,
                                       ClauseReceivingFilter clauseReceivingFilter)
             {


        this.splittingObjectType = splittingObjectType;
        this.splittingObjectFilter = splittingObjectFilter;
        this.splittingObjectSelection = splittingObjectSelection;
        this.splittingStyle = splittingStyle;
        this.formulaGenerator = formulaGenerator;

        this.staticProgramAnalysis = staticProgramAnalysis;
        this.clauseSharingFilter = clauseSharingFilter;
        this.clauseSharingInterval = clauseSharingInterval;
        this.clauseReceivingFilter = clauseReceivingFilter;
        this.splittingIntN = splittingIntN;
        this.splittingIntM = splittingIntM;
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



    private int calculateNrOfSplits(){
        switch(this.splittingStyle){
            case NO_SPLITTING_STYLE:
            case LINEAR_SPLITTING_STYLE:
                return splittingIntN;
            case BINARY_SPLITTING_STYLE:
                return (int) Math.pow(2, splittingIntN);
            case LINEAR_AND_BINARY_SPLITTING_STYLE:
                return splittingIntN * (int) Math.pow(2, splittingIntM);

            default:
                throw (new Error("Formula QueueStyle not supported by ParallelSolverConfiguration Constructor. Can't calculate number of Split."));
        }
    }

    private int calculateFormulaLength(){
        switch(this.splittingStyle){
            case NO_SPLITTING_STYLE:
                return 0;
            case LINEAR_SPLITTING_STYLE:
                return (splittingIntN - 1);
            case BINARY_SPLITTING_STYLE:
                return splittingIntN;
            case LINEAR_AND_BINARY_SPLITTING_STYLE:
                return (splittingIntN - 1 + splittingIntM);
            default:
                throw (new Error("Formula QueueStyle not supported by ParallelSolverConfiguration Constructor. Can't Formula Length."));
        }
    }

    public String toString(){
        String toString = new String();

        toString += splittingStyle + ",";
        toString += splittingIntN + ",";
        toString += splittingIntM + ",";
        toString += numberOfSplits + ",";
        toString += formulaLength + ",";
        toString += maxNumberOfConcurrentThreads + ",";
        toString += splittingObjectType + ",";
        toString += splittingObjectFilter + ",";
        toString += splittingObjectSelection + ",";
        toString += randomSeed + ",";
        toString += staticProgramAnalysis + ",";
        toString += clauseSharingFilter + ",";
        toString += clauseSharingInterval + ",";
        toString += clauseReceivingFilter;





        return toString;
    }

    //----------------------------------SETTER--------------------------------

    public void setChosenEvents(int[] chosenEventCIDs){
        chosenEvents = chosenEventCIDs;
    }

    public void setSplittingStyle(SplittingStyle splittingStyle) {
        this.splittingStyle = splittingStyle;
        this.numberOfSplits = calculateNrOfSplits();
        this.formulaLength = calculateFormulaLength();
    }

    public void setSplittingObjectType(SplittingObjectType splittingObjectType) {
        this.splittingObjectType = splittingObjectType;
    }

    public void setSplittingObjectFilter(SplittingObjectFilter splittingObjectFilter) {
        this.splittingObjectFilter = splittingObjectFilter;
    }

    public void setSplittingObjectSelection(SplittingObjectSelection splittingObjectSelection) {
        this.splittingObjectSelection = splittingObjectSelection;
    }

    public void setStaticProgramAnalysis(StaticProgramAnalysis staticProgramAnalysis) {
        this.staticProgramAnalysis = staticProgramAnalysis;
    }

    public void setFormulaGenerator(FormulaGenerator formulaGenerator) {
        this.formulaGenerator = formulaGenerator;
    }

    public void setClauseSharingFilter(ClauseSharingFilter clauseSharingFilter) {
        this.clauseSharingFilter = clauseSharingFilter;
    }

    public void setClauseReceivingFilter(ClauseReceivingFilter clauseReceivingFilter) {
        this.clauseReceivingFilter = clauseReceivingFilter;
    }

    public void setSplittingIntN(int splittingIntN) {
        this.splittingIntN = splittingIntN;
        this.numberOfSplits = calculateNrOfSplits();
        this.formulaLength = calculateFormulaLength();
    }

    public void setSplittingIntM(int splittingIntM) {
        this.splittingIntM = splittingIntM;
        this.numberOfSplits = calculateNrOfSplits();
        this.formulaLength = calculateFormulaLength();
    }

    public void setMaxNumberOfConcurrentThreads(int maxNumberOfConcurrentThreads) {
        this.maxNumberOfConcurrentThreads = maxNumberOfConcurrentThreads;
    }

    public void setRandomSeed(long randomSeed) {
        this.randomSeed = randomSeed;
    }

    public void setClauseSharingInterval(int clauseSharingInterval){
        this.clauseSharingInterval = clauseSharingInterval;
    }

    public void initializeFileReport(String reportFileName, String architectureName, String targetName, String solverName){
        this.initialisedFileReport = true;
        this.reportFileName = reportFileName;
        this.architectureName = architectureName;
        this.targetName = targetName;
        this.solverName = solverName;
    }

    //----------------------------------GETTER--------------------------------

    public SplittingObjectType getSplittingObjectType() {
        return splittingObjectType;
    }

    public SplittingObjectFilter getSplittingObjectFilter() {
        return splittingObjectFilter;
    }

    public SplittingObjectSelection getSplittingObjectSelection() {
        return splittingObjectSelection;
    }

    public SplittingStyle getSplittingStyle() {
        return splittingStyle;
    }

    public StaticProgramAnalysis getStaticProgramAnalysis() {return staticProgramAnalysis;}

    public FormulaGenerator getFormulaGenerator() {
        return formulaGenerator;
    }

    public ClauseSharingFilter getClauseSharingFilter() {
        return clauseSharingFilter;
    }

    public ClauseReceivingFilter getClauseReceivingFilter() {
        return clauseReceivingFilter;
    }

    public int getSplittingIntN() {
        return splittingIntN;
    }

    public int getSplittingIntM() {
        return splittingIntM;
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

    public int[] getChosenEvents() {return chosenEvents;}

    public int getClauseSharingInterval() {
        return clauseSharingInterval;
    }

    public boolean isInitialisedFileReport() {
        return initialisedFileReport;
    }

    public String getReportFileName() {
        return reportFileName;
    }

    public String getArchitectureName() {
        return architectureName;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getSolverName() {
        return solverName;
    }
}

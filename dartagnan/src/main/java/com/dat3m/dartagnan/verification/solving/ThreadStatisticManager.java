package com.dat3m.dartagnan.verification.solving;

import com.dat3m.dartagnan.utils.Result;
import org.sosy_lab.java_smt.api.BooleanFormula;

import java.util.BitSet;

public class ThreadStatisticManager {
    private final int threadID;
    private final ParallelSolverConfiguration parallelConfig;
    private BooleanFormula  myFormula;
    private Result myResult;
    private BitSet[] bitSetPair;
    private boolean resultReported;

    private long startTime = -1;
    private long endTime = -1;

    private long preProcessingTime = -1;

    private long totalWMMSolverTime = 0;
    private long totalCAATSolverTime= 0;
    private long totalSolverTime= 0;


    private long clauseSharingTime = 0;
    private long clauseSharingFilterTime = 0;
    private int clauseSharingFilterCount = 0;


    private long clauseReceivingTime = 0;
    private long clauseReceivingFilterTime = 0;
    private int clauseReceivingFilterCount = 0;


    //Constructor
    public ThreadStatisticManager(int threadID, ParallelSolverConfiguration parallelConfig){
        this.threadID = threadID;
        this.parallelConfig = parallelConfig;
        bitSetPair = new BitSet[]{new BitSet(), new BitSet()};
        this.resultReported = false;

    }


    //..............Print...................
    public void printThreadStatistics(){
        if(!resultReported){
            System.out.println("Thread " + threadID + " did not finish calculations and aborted.\n \n");
            return;
        }

        printGeneralInfo();

        System.out.println("");
        printSolverTime();

        if(parallelConfig.getClauseSharingFilter() != ParallelSolverConfiguration.ClauseSharingFilter.NO_CLAUSE_SHARING) {
            System.out.println("");
            printClauseSharingStats();
            System.out.println("");
            printClauseReceivingStats();
        }

        System.out.println("\n");
    }

    public void printGeneralInfo(){
        if(!resultReported){
            System.out.println("Thread " + threadID + " did not finish calculations and aborted.\n \n");
            return;
        }
        System.out.println("Thread " + threadID + " report:");


        System.out.println("TotalTime: " + (int)(calculateTotalTime()/1000) + " seconds");
        System.out.println("Formula: " + myFormula);
        System.out.println("Result " + myResult.name());
    }

    public void printSolverTime(){
        if(!resultReported){
            System.out.println("Thread " + threadID + " did not finish calculations and aborted.\n \n");
            return;
        }

        System.out.println("Thread " + threadID + " Solver Times:");
        System.out.println("TotalTime: " + (int)(calculateTotalTime()/1000) + " seconds");
        System.out.println("Preprocessing Time: " + toSeconds(preProcessingTime) + " seconds");
        System.out.println("TotalSolverTime: " + toSeconds(totalSolverTime) + " seconds");
        if(totalCAATSolverTime > 0){System.out.println("TotalCAATSolverTime: " + toSeconds(totalCAATSolverTime) + " seconds");}
        if(totalWMMSolverTime > 0){System.out.println("TotalWMMSolverTime: " + toSeconds(totalWMMSolverTime) + " seconds");}

    }

    public void printClauseSharingStats(){
        if(!resultReported){
            System.out.println("Thread " + threadID + " did not finish calculations and aborted.\n \n");
            return;
        }

        System.out.println("Thread " + threadID + " ClauseSharingStats: ");
        System.out.println("ClauseSharingTime: " + clauseSharingTime + " ms");
        if(parallelConfig.getClauseSharingFilter() != ParallelSolverConfiguration.ClauseSharingFilter.NO_CS_FILTER){
            System.out.println("ClauseSharingFilterTime: " + clauseSharingFilterTime + " ms");
            System.out.println("Shared Clauses Filtered: " + clauseSharingFilterCount);
        }
    }

    public void printClauseReceivingStats(){
        if(!resultReported){
            System.out.println("Thread " + threadID + " did not finish calculations and aborted.\n \n");
            return;
        }

        System.out.println("Thread " + threadID + " ClauseReceivingStats: ");
        System.out.println("ClauseReceivingTime: " + clauseReceivingTime + " ms");
        if(parallelConfig.getClauseReceivingFilter() != ParallelSolverConfiguration.ClauseReceivingFilter.NO_CR_FILTER) {
            System.out.println("ClauseReceivingFilterTime: " + clauseReceivingFilterTime + " ms");
            System.out.println("Received Clauses Filtered: " + clauseReceivingFilterCount);
        }
    }

    public int toSeconds(long timeInMillis){
        return ((int)(timeInMillis/1000));
    }

    //..............Derived..Stats............
    public long calculateTotalTime(){
        return endTime-startTime;
    }


    //..................Report..Stats............
    public void reportResult(Result myResult){
        resultReported = true;
        endTime = System.currentTimeMillis();
        this.myResult = myResult;
    }

    public void reportStart(){
        this.startTime = System.currentTimeMillis();
    }

    public void reportPreprocessingTime(){
        this.preProcessingTime = System.currentTimeMillis() - this.startTime;
    }

    public String reportString(){
        if(!resultReported){
            return "-1, -1, -1, -1, -1, -1, -1";
        }

        String reportString;

        reportString = bitSetPair[0].toString().replaceAll(",", "")
                + "," + bitSetPair[1].toString().replaceAll(",", "")
                + "," + startTime
                + "," + endTime
                + "," + totalSolverTime
                + "," + totalWMMSolverTime
                + "," + totalCAATSolverTime
        ;

    return reportString;
    }

    //..............Add..Time..Methods..........
    public void addWMMSolverTime(long wmmTime){
        totalWMMSolverTime += wmmTime;
        totalSolverTime += wmmTime;
    }

    public void addCAATSolverTime(long caatTime){
        totalCAATSolverTime += caatTime;
        totalSolverTime += caatTime;
    }

    public void addAssumeSolverTime(long assumeTime){
        totalSolverTime+= assumeTime;
    }

    public void addClauseSharingTime(long cs_time){
        clauseSharingTime += cs_time;
    }

    public void addClauseSharingFilterTime(long csf_time){
        clauseSharingFilterTime += csf_time;
    }

    public void addClauseReceivingTime(long cr_time){
        clauseReceivingTime += cr_time;
    }

    public void addClauseReceivingFilterTime(long csf_time){
        clauseReceivingFilterTime += csf_time;
    }



    //................Count..Methods............

    public void clauseSharingCountInc(int filteredCount){
        clauseSharingFilterCount += filteredCount;
    }

    public void clauseReceivingCountInc(int filteredCount){
        clauseReceivingFilterCount += filteredCount;
    }





    //....................GETTER..SETTER.............
    public void setBitSetPair(BitSet[] bitSetPair){
        this.bitSetPair = bitSetPair;
    }

    public void setMyFormula(BooleanFormula myFormula){
        this.myFormula = myFormula;
    }

    public BitSet[] getBitSetPair(){
        return bitSetPair;
    }

    public long getTotalTime(){
        return calculateTotalTime();
    }


}

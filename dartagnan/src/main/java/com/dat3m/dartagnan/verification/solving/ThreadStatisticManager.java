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


    private double startTime = -1;
    private double endTime = -1;

    private double preProcessingTime = -1;

    private double totalWMMSolverTime = 0;
    private double totalCAATSolverTime= 0;
    private double totalSolverTime= 0;

    private double clauseSharingTime = 0;
    private double clauseSharingFilterTime = 0;
    private int clauseSharingFilterCount = 0;


    private double clauseReceivingTime = 0;
    private double clauseReceivingFilterTime = 0;
    private int clauseReceivingFilterCount = 0;


    //Constructor
    public ThreadStatisticManager(int threadID, ParallelSolverConfiguration parallelConfig){
        this.threadID = threadID;
        this.parallelConfig = parallelConfig;

    }


    //..............Print...................
    public void print(){
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
        System.out.println("Thread " + threadID + " report:");


        System.out.println("TotalTime: " + (int)(calculateTotalTime()/1000) + " seconds");
        System.out.println("Formula: " + myFormula);
        System.out.println("Result " + myResult.name());
    }

    public void printSolverTime(){
        System.out.println("Thread " + threadID + " Solver Times:");
        System.out.println("TotalTime: " + (int)(calculateTotalTime()/1000) + " seconds");
        System.out.println("Preprocessing Time: " + toSeconds(preProcessingTime) + " seconds");
        System.out.println("TotalSolverTime: " + toSeconds(totalSolverTime) + " seconds");
        if(totalCAATSolverTime > 0){System.out.println("TotalCAATSolverTime: " + toSeconds(totalCAATSolverTime) + " seconds");}
        if(totalWMMSolverTime > 0){System.out.println("TotalWMMSolverTime: " + toSeconds(totalWMMSolverTime) + " seconds");}

    }

    public void printClauseSharingStats(){
        System.out.println("Thread " + threadID + " ClauseSharingStats: ");
        System.out.println("ClauseSharingTime: " + clauseSharingTime + " ms");
        if(parallelConfig.getClauseSharingFilter() != ParallelSolverConfiguration.ClauseSharingFilter.NO_CS_FILTER){
            System.out.println("ClauseSharingFilterTime: " + clauseSharingFilterTime + " ms");
            System.out.println("Shared Clauses Filtered: " + clauseSharingFilterCount);
        }
    }

    public void printClauseReceivingStats(){
        System.out.println("Thread " + threadID + " ClauseReceivingStats: ");
        System.out.println("ClauseReceivingTime: " + clauseReceivingTime + " ms");
        if(parallelConfig.getClauseReceivingFilter() != ParallelSolverConfiguration.ClauseReceivingFilter.NO_CR_FILTER) {
            System.out.println("ClauseReceivingFilterTime: " + clauseReceivingFilterTime + " ms");
            System.out.println("Received Clauses Filtered: " + clauseReceivingFilterCount);
        }
    }

    public int toSeconds(double timeInMillis){
        return ((int)(timeInMillis/1000));
    }

    //..............Derived..Stats............
    public double calculateTotalTime(){
        return endTime-startTime;
    }


    //..................Report..Stats............
    public void reportResult(Result myResult){
        endTime = System.currentTimeMillis();
        this.myResult = myResult;
    }

    public void reportStart(){
        this.startTime = System.currentTimeMillis();
    }

    public void reportPreprocessingTime(){
        this.preProcessingTime = System.currentTimeMillis() - this.startTime;
    }

    //..............Add..Time..Methods..........
    public void addWMMSolverTime(double wmmTime){
        totalWMMSolverTime += wmmTime;
        totalSolverTime += wmmTime;
    }

    public void addCAATSolverTime(double caatTime){
        totalCAATSolverTime += caatTime;
        totalSolverTime += caatTime;
    }

    public void addAssumeSolverTime(double assumeTime){
        totalSolverTime+= assumeTime;
    }

    public void addClauseSharingTime(double cs_time){
        clauseSharingTime += cs_time;
    }

    public void addClauseSharingFilterTime(double csf_time){
        clauseSharingFilterTime += csf_time;
    }

    public void addClauseReceivingTime(double cr_time){
        clauseReceivingTime += cr_time;
    }

    public void addClauseReceivingFilterTime(double csf_time){
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

    public double getTotalTime(){
        return calculateTotalTime();
    }


}

package com.dat3m.dartagnan.verification.solving;

import com.dat3m.dartagnan.utils.Result;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class ThreadStatisticManager {
    private final int threadID;
    private double startTime;
    private double endTime = -1;
    private BooleanFormula  myFormula;
    private Result myResult;


    public ThreadStatisticManager(int threadID){
        this.threadID = threadID;

    }

    public int calculateTotalTime(){
        return (int)((endTime-startTime)/1000);
    }

    public void reportResult(Result myResult){
        endTime = System.currentTimeMillis();
        this.myResult = myResult;
    }

    public void reportStart(){
        this.startTime = System.currentTimeMillis();
    }

    public void setMyFormula(BooleanFormula myFormula){
        this.myFormula = myFormula;
    }


    public void print(){
        System.out.println("Thread " + threadID + " report:");

        if(endTime != (-1)){
            System.out.println("TotalTime: " + calculateTotalTime() + " seconds");
        }
        System.out.println("Formula: " + myFormula);
        System.out.println("\n");
    }


}

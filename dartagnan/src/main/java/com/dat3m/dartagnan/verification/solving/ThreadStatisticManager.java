package com.dat3m.dartagnan.verification.solving;

import com.dat3m.dartagnan.utils.Result;
import org.sosy_lab.java_smt.api.BooleanFormula;

import java.util.BitSet;

public class ThreadStatisticManager {
    private final int threadID;
    private double startTime;
    private double endTime = -1;
    private BooleanFormula  myFormula;
    private Result myResult;
    private BitSet[] bitSetPair;






    public ThreadStatisticManager(int threadID){
        this.threadID = threadID;

    }

    public double calculateTotalTime(){
        return endTime-startTime;
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
            System.out.println("TotalTime: " + (int)(calculateTotalTime()/1000) + " seconds");
        }
        System.out.println("Formula: " + myFormula);
        System.out.println("\n");
    }

    public void setBitSetPair(BitSet[] bitSetPair){
        this.bitSetPair = bitSetPair;
    }

    public BitSet[] getBitSetPair(){
        return bitSetPair;
    }

    public double getTotalTime(){
        return calculateTotalTime();
    }


}

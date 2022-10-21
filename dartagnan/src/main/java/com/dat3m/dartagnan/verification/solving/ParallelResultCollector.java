package com.dat3m.dartagnan.verification.solving;

import com.dat3m.dartagnan.utils.Result;

public class ParallelResultCollector {

    private int numberOfFinishedThreads;
    private Result aggregatedResult;
    private int maxConcurrentThreads;
    private int currentlyRunningThreads;
    private long[] finishTimeCollector;

    ParallelResultCollector(){
        this.numberOfFinishedThreads = 0;
        this.aggregatedResult = Result.PASS;
        this.currentlyRunningThreads = 0;
        this.maxConcurrentThreads = 0;
    }



    ParallelResultCollector(Result result, int numberOfFinishedThreads, int maxConcurrentThreads, int totalThreadAmount){
        this.numberOfFinishedThreads = numberOfFinishedThreads;
        this.aggregatedResult = result;
        this.currentlyRunningThreads = 0;
        this.maxConcurrentThreads = maxConcurrentThreads;
        this.finishTimeCollector = new long[totalThreadAmount];
    }


    public synchronized void updateResult(Result result,int threadID, long startTime){
        numberOfFinishedThreads++;
        currentlyRunningThreads--;
        finishTimeCollector[threadID] = System.currentTimeMillis() - startTime;
        if(result == Result.UNKNOWN){
            if(aggregatedResult == Result.PASS){
                aggregatedResult = Result.UNKNOWN;
            }
        }
        if(result == Result.FAIL){
            aggregatedResult = Result.FAIL;
        }
    }

    public synchronized boolean canAddThread(){
        if(currentlyRunningThreads < maxConcurrentThreads){
            currentlyRunningThreads++;
            return true;
        }
        return false;
    }

    public synchronized Result getAggregatedResult(){
        return aggregatedResult;
    }

    public synchronized int getNumberOfFinishedThreads(){
        return numberOfFinishedThreads;
    }

    public void printTimes(){
        for(int i = 0; i < finishTimeCollector.length; i++){
            int printTime = (int)finishTimeCollector[i] / 1000;
            System.out.println("Thread " + i + " took " + printTime + " seconds.");
        }

    }

    public void setMaxConcurrentThreads(int maxConcurrentThreads){
        this.maxConcurrentThreads = maxConcurrentThreads;
    }
}



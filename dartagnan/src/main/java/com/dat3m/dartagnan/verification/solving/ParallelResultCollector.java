package com.dat3m.dartagnan.verification.solving;

import com.dat3m.dartagnan.utils.Result;

public class ParallelResultCollector {

    private int numberOfFinishedThreads;
    private Result aggregatedResult;
    private int maxNumberOfThreads;
    private int currentNumberOfThreads;
    private long[] finishTimeCollector;

    ParallelResultCollector(){
        this.numberOfFinishedThreads = 0;
        this.aggregatedResult = Result.PASS;
        this.currentNumberOfThreads = 0;
        this.maxNumberOfThreads = 6;
    }

    ParallelResultCollector(Result result, int numberOfFinishedThreads){
        this.numberOfFinishedThreads = numberOfFinishedThreads;
        this.aggregatedResult = result;
        this.currentNumberOfThreads = 0;
        this.maxNumberOfThreads = 6;
    }

    ParallelResultCollector(Result result, int numberOfFinishedThreads, int maxNumberOfThreads, int totalThreadNumber){
        this.numberOfFinishedThreads = numberOfFinishedThreads;
        this.aggregatedResult = result;
        this.currentNumberOfThreads = 0;
        this.maxNumberOfThreads = maxNumberOfThreads;
        this.finishTimeCollector = new long[totalThreadNumber];
    }


    public synchronized void updateResult(Result result,int threadID, long startTime){
        numberOfFinishedThreads++;
        currentNumberOfThreads--;
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
        if(currentNumberOfThreads < maxNumberOfThreads){
            currentNumberOfThreads++;
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
}



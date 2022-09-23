package com.dat3m.dartagnan.verification.solving;

import com.dat3m.dartagnan.utils.Result;

public class ParallelResultCollector {

    private int numberOfFinishedThreads;
    private Result aggregatedResult;
    private int maxNumberOfThreads;
    private int currentNumberOfThreads;

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

    ParallelResultCollector(Result result, int numberOfFinishedThreads, int maxNumberOfThreads){
        this.numberOfFinishedThreads = numberOfFinishedThreads;
        this.aggregatedResult = result;
        this.currentNumberOfThreads = 0;
        this.maxNumberOfThreads = maxNumberOfThreads;
    }


    public synchronized void updateResult(Result result){
        numberOfFinishedThreads++;
        currentNumberOfThreads--;
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
}



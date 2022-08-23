package com.dat3m.dartagnan.verification.solving;

import com.dat3m.dartagnan.utils.Result;

public class parallelResultCollector {

    private int numberOfFinishedThreads;
    private Result aggregatedResult;

    parallelResultCollector(){
        this.numberOfFinishedThreads = 0;
        this.aggregatedResult = Result.PASS;
    }

    parallelResultCollector(Result result, int numberOfFinishedThreads){
        this.numberOfFinishedThreads = numberOfFinishedThreads;
        this.aggregatedResult = result;
    }


    public synchronized void updateResult(Result result){
        numberOfFinishedThreads++;
        if(result == Result.UNKNOWN){
            if(aggregatedResult == Result.PASS){
                aggregatedResult = Result.UNKNOWN;
            }
        }
        if(result == Result.FAIL){
            aggregatedResult = Result.FAIL;
        }
    }

    public synchronized Result getAggregatedResult(){
        return aggregatedResult;
    }

    public synchronized int getNumberOfFinishedThreads(){
        return numberOfFinishedThreads;
    }
}



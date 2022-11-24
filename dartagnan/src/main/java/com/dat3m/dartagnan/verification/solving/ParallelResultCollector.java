package com.dat3m.dartagnan.verification.solving;

import com.dat3m.dartagnan.utils.Result;

public class ParallelResultCollector {

    private int numberOfFinishedThreads;
    private Result aggregatedResult;
    private final int maxConcurrentThreads;
    private int currentlyRunningThreads;
    private ThreadStatisticManager[] statisticManagers;
    private final ParallelSolverConfiguration parallelConfig;


    ParallelResultCollector(Result result, ParallelSolverConfiguration parallelConfig){
        this.numberOfFinishedThreads = 0;
        this.aggregatedResult = result;
        this.currentlyRunningThreads = 0;
        this.parallelConfig = parallelConfig;
        this.maxConcurrentThreads = this.parallelConfig.getMaxNumberOfConcurrentThreads();
        this.statisticManagers = new ThreadStatisticManager[parallelConfig.getNumberOfSplits()];
    }


    public synchronized void updateResult(Result result, int threadID, ThreadStatisticManager statisticManager){
        statisticManagers[threadID] = statisticManager;
        numberOfFinishedThreads++;
        currentlyRunningThreads--;
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
        for(ThreadStatisticManager tSM: statisticManagers){
            tSM.print();
        }
    }

}



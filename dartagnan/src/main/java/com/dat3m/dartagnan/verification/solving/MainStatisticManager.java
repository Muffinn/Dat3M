package com.dat3m.dartagnan.verification.solving;

import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.wmm.utils.Tuple;

import java.util.BitSet;
import java.util.List;

public class MainStatisticManager {
    private final ThreadStatisticManager[] threadStatisticManagers;
    private final ParallelSolverConfiguration parallelConfig;
    private final SplittingManager fqmgr;

    public MainStatisticManager(int numberOfSplits, ParallelSolverConfiguration parallelConfig, SplittingManager splittingManager){
        threadStatisticManagers = new ThreadStatisticManager[numberOfSplits];
        this.parallelConfig = parallelConfig;
        this.fqmgr = splittingManager;

        for (int i = 0; i < numberOfSplits; i++){
            threadStatisticManagers[i] = new ThreadStatisticManager(i);
        }
    }

    public ThreadStatisticManager getThreadStatisticManager(int threadID){
        return threadStatisticManagers[threadID];
    }


    public void printThreadStatistics(){
        for(ThreadStatisticManager tSM: threadStatisticManagers){
            tSM.print();
        }
        printLiteralStatistics();
    }

    public void printLiteralStatistics(){
        if(parallelConfig.getSplittingObjectType() == ParallelSolverConfiguration.SplittingObjectType.NO_SPLITTING_OBJECTS){
            System.out.println("No Literals -> no Literal Statistic. :)");
        }

        int formulaLength = parallelConfig.getFormulaLength();
        int[] literalScore = new int[formulaLength];
        double[] totalTrueTime = new double[formulaLength];
        double[] totalFalseTime = new double[formulaLength];

        int numberOfSplits = parallelConfig.getNumberOfSplits();
        for (int i = 0; i < numberOfSplits; i++){
            BitSet[] bitSetPair = threadStatisticManagers[i].getBitSetPair();
            for (int j = 0; j < formulaLength; j++){
                if(bitSetPair[0].get(j)){
                    totalTrueTime[j] += threadStatisticManagers[i].getTotalTime();
                }
                if(bitSetPair[1].get(j)){
                    totalFalseTime[j] += threadStatisticManagers[i].getTotalTime();
                }

            }

        }
        for (int j = 0; j < formulaLength; j++) {
            literalScore[j] = (int) (100 * totalTrueTime[j] / (totalFalseTime[j] + totalTrueTime[j]));
        }

        switch (parallelConfig.getSplittingObjectType()){
            case EVENT_SPLITTING_OBJECTS:
                List<Event> eventList = fqmgr.getEventList();
                System.out.println("Event Scores:");
                for (int i = 0; i < formulaLength; i++){
                    System.out.println("Event " + eventList.get(i).getCId() + " :");
                    System.out.println("TotalTrueTime: " + (int)(totalTrueTime[i]/1000) + " seconds");
                    System.out.println("TotalFalseTime: " + (int)(totalFalseTime[i]/1000) + " seconds");
                    System.out.println("Score :" + literalScore[i] + "\n");
                }
                break;

            case CO_RELATION_SPLITTING_OBJECTS:
                List<Tuple> coTupleList = fqmgr.getTupleList();
                System.out.println("Event Scores:");
                for (int i = 0; i < formulaLength; i++){
                    System.out.println("RF-Tuple " + coTupleList.get(i).getFirst().getCId() + ", " + coTupleList.get(i).getSecond().getCId() + " :");
                    System.out.println("TotalTrueTime: " + (int)(totalTrueTime[i]/1000) + " seconds");
                    System.out.println("TotalFalseTime: " + (int)(totalFalseTime[i]/1000) + " seconds");
                    System.out.println("Score: " + literalScore[i] + "\n");
                }
                break;
            case RF_RELATION_SPLITTING_OBJECTS:
                List<Tuple> rfTupleList = fqmgr.getTupleList();
                System.out.println("Event Scores:");
                for (int i = 0; i < formulaLength; i++){
                    System.out.println("CO-Tuple " + rfTupleList.get(i).getFirst().getCId() + ", " + rfTupleList.get(i).getSecond().getCId() + " :");
                    System.out.println("TotalTrueTime: " + (int)(totalTrueTime[i]/1000) + " seconds");
                    System.out.println("TotalFalseTime: " + (int)(totalFalseTime[i]/1000) + " seconds");
                    System.out.println("Score: " + literalScore[i] + "\n");
                }
                break;


            default:
                throw(new Error("Unreachable code reached in MainStatisticManager::calculateLiteralStatistics()"));
        }

    }
}

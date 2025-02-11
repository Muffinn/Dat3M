package com.dat3m.dartagnan.verification.solving;

import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.utils.Result;
import com.dat3m.dartagnan.wmm.utils.Tuple;

import java.io.FileWriter;
import java.util.BitSet;
import java.util.Calendar;
import java.util.List;

import java.io.PrintWriter;

public class MainStatisticManager {
    private final ThreadStatisticManager[] threadStatisticManagers;
    private final ParallelSolverConfiguration parallelConfig;
    private final SplittingManager spmgr;
    private Result myResult = Result.UNKNOWN;

    private long startTime = -1;
    private long endTime = -1;



    public MainStatisticManager(int numberOfSplits, ParallelSolverConfiguration parallelConfig, SplittingManager splittingManager){
        threadStatisticManagers = new ThreadStatisticManager[numberOfSplits];
        this.parallelConfig = parallelConfig;
        this.spmgr = splittingManager;

        for (int i = 0; i < numberOfSplits; i++){
            threadStatisticManagers[i] = new ThreadStatisticManager(i, parallelConfig);
        }
    }

    public ThreadStatisticManager getThreadStatisticManager(int threadID){
        return threadStatisticManagers[threadID];
    }


    public void printStatistics(){
        for(ThreadStatisticManager tSM: threadStatisticManagers){
            tSM.printThreadStatistics();
        }
        printLiteralStatistics();
        createReportFile();
    }

    public void printLiteralStatistics(){
        if(parallelConfig.getSplittingObjectType() == ParallelSolverConfiguration.SplittingObjectType.NO_SPLITTING_OBJECTS){
            System.out.println("No Literals -> no Literal Statistic. :)");
        }

        int formulaLength = parallelConfig.getFormulaLength();
        int[] literalScore = new int[formulaLength];
        long[] totalTrueTime = new long[formulaLength];
        long[] totalFalseTime = new long[formulaLength];

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
            case BRANCH_EVENTS_SPLITTING_OBJECTS:
            case ALL_EVENTS_SPLITTING_OBJECTS:
                List<Event> eventList = spmgr.getEventList();
                System.out.println("Event Scores:");
                for (int i = 0; i < formulaLength; i++){
                    System.out.println("Event " + eventList.get(i).getCId() + " :");
                    System.out.println("TotalTrueTime: " + (int)(totalTrueTime[i]/1000) + " seconds");
                    System.out.println("TotalFalseTime: " + (int)(totalFalseTime[i]/1000) + " seconds");
                    System.out.println("Score :" + literalScore[i] + "\n");
                }
                break;

            case CO_RELATION_SPLITTING_OBJECTS:
                List<Tuple> coTupleList = spmgr.getTupleList();
                System.out.println("Event Scores:");
                for (int i = 0; i < formulaLength; i++){
                    System.out.println("CO-Tuple " + coTupleList.get(i).getFirst().getCId() + ", " + coTupleList.get(i).getSecond().getCId() + " :");
                    System.out.println("TotalTrueTime: " + (int)(totalTrueTime[i]/1000) + " seconds");
                    System.out.println("TotalFalseTime: " + (int)(totalFalseTime[i]/1000) + " seconds");
                    System.out.println("Score: " + literalScore[i] + "\n");
                }
                break;
            case RF_RELATION_SPLITTING_OBJECTS:
                List<Tuple> rfTupleList = spmgr.getTupleList();
                System.out.println("Event Scores:");
                for (int i = 0; i < formulaLength; i++){
                    System.out.println("RF-Tuple " + rfTupleList.get(i).getFirst().getCId() + ", " + rfTupleList.get(i).getSecond().getCId() + " :");
                    System.out.println("TotalTrueTime: " + (int)(totalTrueTime[i]/1000) + " seconds");
                    System.out.println("TotalFalseTime: " + (int)(totalFalseTime[i]/1000) + " seconds");
                    System.out.println("Score: " + literalScore[i] + "\n");
                }
                break;
            case NO_SPLITTING_OBJECTS:
                break;


            default:
                throw(new Error("Unreachable code reached in MainStatisticManager::calculateLiteralStatistics()"));
        }

    }

    public void reportStart(){
        this.startTime = System.currentTimeMillis();
    }


    public void reportResult(Result myResult){
        endTime = System.currentTimeMillis();
        this.myResult = myResult;
    }


    private long calcTotalTime(){
        return (endTime - startTime);
    }

    public void createReportFile(){
        if (!parallelConfig.isInitialisedFileReport()){
            return;
        }
        Calendar date = Calendar.getInstance();
        String reportFileName = parallelConfig.getReportFileName() + "_" + parallelConfig.getSolverName() + "_"
                + parallelConfig.getTargetName() + "_" + parallelConfig.getArchitectureName();


        String fullName = "output/reports/" + reportFileName + ".csv";
        StringBuilder sb = new StringBuilder();

        sb.append(parallelConfig.getReportFileName());
        sb.append(",");
        sb.append(parallelConfig.getSolverName());
        sb.append(",");
        sb.append(parallelConfig.getTargetName());
        sb.append(",");
        sb.append(parallelConfig.getArchitectureName());
        sb.append(",");
        sb.append(date.get(Calendar.DAY_OF_MONTH));
        sb.append(".");
        sb.append(date.get(Calendar.MONTH));
        sb.append(".");
        sb.append(date.get(Calendar.YEAR));
        sb.append(",");

        sb.append(parallelConfig);
        sb.append(",");

        int formulaLength = parallelConfig.getFormulaLength();
        sb.append("{");
        switch (parallelConfig.getSplittingObjectType()){
            case BRANCH_EVENTS_SPLITTING_OBJECTS:
            case ALL_EVENTS_SPLITTING_OBJECTS:
                List<Event> eventList = spmgr.getEventList();
                for (int i = 0; i < formulaLength; i++){
                    sb.append(eventList.get(i).getCId());
                    sb.append(" ");
                }
                break;

            case CO_RELATION_SPLITTING_OBJECTS:
            case RF_RELATION_SPLITTING_OBJECTS:
                List<Tuple> tupleList = spmgr.getTupleList();

                for (int i = 0; i < formulaLength; i++){
                    sb.append("[");
                    sb.append(tupleList.get(i).getFirst().getCId());
                    sb.append(" ");
                    sb.append(tupleList.get(i).getSecond().getCId());
                    sb.append("]");
                }
                break;

            case NO_SPLITTING_OBJECTS:
                break;

            default:
                throw(new Error("Unreachable code reached in MainStatisticManager::calculateLiteralStatistics()"));
        }
        sb.append("},");
        sb.append(spmgr.getFilteredLiterals());
        sb.append(",");

        sb.append("timeStats:,");
        sb.append(startTime);
        sb.append(",");
        sb.append(endTime);
        sb.append(",");
        for(ThreadStatisticManager tsmtsmtsm : threadStatisticManagers) {
            sb.append(tsmtsmtsm.reportString());
            sb.append(",");
        }

        try(FileWriter fileWriter = new FileWriter(fullName, true);
             PrintWriter printWriter = new PrintWriter(fileWriter);) {
            printWriter.println(sb);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}

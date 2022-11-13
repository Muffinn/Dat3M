package com.dat3m.dartagnan.verification.solving;

import org.sosy_lab.common.configuration.InvalidConfigurationException;

public class SeedLeaderboard {

    public static ParallelSolverConfiguration Dglm3TsoLeaderboard(int rank){
        ParallelSolverConfiguration parallelConfig = null;
        try{switch (rank){

            default:
            case 1:
                //22.836 Seconds  602 20 437
                parallelConfig = new ParallelSolverConfiguration(ParallelSolverConfiguration.FormulaItemType.EVENT_FORMULAS,
                        ParallelSolverConfiguration.FormulaItemFilter.MUTUALLY_EXCLUSIVE_FILTER,
                        ParallelSolverConfiguration.FormulaItemOrder.SEEDED_RANDOM_ORDER,
                        ParallelSolverConfiguration.FormulaQueueStyle.TREE_SHAPED_FORMULA_QUEUE,
                        ParallelSolverConfiguration.FormulaGeneration.IN_SOLVER,
                        ParallelSolverConfiguration.ClauseSharingFilter.NO_FILTER,
                        2,
                        2,
                        4,
                        -8235967333839908600L);

        }}catch(Exception e){
            System.out.println("Unreachable code reached. Seed Leaderboard DLM-3 Tso Rank " + rank + " failed. Error Message:" + e.getMessage());
        }
        return parallelConfig;
    }

    public static ParallelSolverConfiguration Dglm3Arm8Leaderboard(int rank){
        ParallelSolverConfiguration parallelConfig = null;
        try{switch (rank){

            default:
            case 1:
                //24.188 Seconds
                parallelConfig = new ParallelSolverConfiguration(ParallelSolverConfiguration.FormulaItemType.EVENT_FORMULAS,
                        ParallelSolverConfiguration.FormulaItemFilter.MUTUALLY_EXCLUSIVE_FILTER,
                        ParallelSolverConfiguration.FormulaItemOrder.SEEDED_RANDOM_ORDER,
                        ParallelSolverConfiguration.FormulaQueueStyle.TREE_SHAPED_FORMULA_QUEUE,
                        ParallelSolverConfiguration.FormulaGeneration.IN_SOLVER,
                        ParallelSolverConfiguration.ClauseSharingFilter.NO_FILTER,
                        2,
                        2,
                        4,
                        6274155622082046524L);

        }}catch(Exception e){
            System.out.println("Unreachable code reached. Seed Leaderboard DLM-3 Tso Rank " + rank + " failed. Error Message:" + e.getMessage());
        }
        return parallelConfig;
    }


    public static ParallelSolverConfiguration Ms3TsoLeaderboard(int rank){
        ParallelSolverConfiguration parallelConfig = null;
        try{switch (rank){

            default:
            case 1:
                //47.375 Seconds
                parallelConfig = new ParallelSolverConfiguration(ParallelSolverConfiguration.FormulaItemType.EVENT_FORMULAS,
                        ParallelSolverConfiguration.FormulaItemFilter.MUTUALLY_EXCLUSIVE_FILTER,
                        ParallelSolverConfiguration.FormulaItemOrder.SEEDED_RANDOM_ORDER,
                        ParallelSolverConfiguration.FormulaQueueStyle.TREE_SHAPED_FORMULA_QUEUE,
                        ParallelSolverConfiguration.FormulaGeneration.IN_SOLVER,
                        ParallelSolverConfiguration.ClauseSharingFilter.NO_FILTER,
                        2,
                        2,
                        4,
                        -2372824711601646356L);

        }}catch(Exception e){
            System.out.println("Unreachable code reached. Seed Leaderboard DLM-3 Tso Rank " + rank + " failed. Error Message:" + e.getMessage());
        }
        return parallelConfig;
    }

    public static ParallelSolverConfiguration Ms3Arm8Leaderboard(int rank){
        ParallelSolverConfiguration parallelConfig = null;
        try{switch (rank){

            default:
            case 1:
                //27.191 Seconds
                parallelConfig = new ParallelSolverConfiguration(ParallelSolverConfiguration.FormulaItemType.EVENT_FORMULAS,
                        ParallelSolverConfiguration.FormulaItemFilter.MUTUALLY_EXCLUSIVE_FILTER,
                        ParallelSolverConfiguration.FormulaItemOrder.SEEDED_RANDOM_ORDER,
                        ParallelSolverConfiguration.FormulaQueueStyle.TREE_SHAPED_FORMULA_QUEUE,
                        ParallelSolverConfiguration.FormulaGeneration.IN_SOLVER,
                        ParallelSolverConfiguration.ClauseSharingFilter.NO_FILTER,
                        2,
                        2,
                        4,
                        4081063120902846059L);

        }}catch(Exception e){
            System.out.println("Unreachable code reached. Seed Leaderboard DLM-3 Tso Rank " + rank + " failed. Error Message:" + e.getMessage());
        }
        return parallelConfig;
    }
}

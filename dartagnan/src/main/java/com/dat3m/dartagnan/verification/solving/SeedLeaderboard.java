package com.dat3m.dartagnan.verification.solving;

public class SeedLeaderboard {

    public static ParallelSolverConfiguration Dglm3TsoLeaderboard(int rank){
        ParallelSolverConfiguration parallelConfig = null;
        try{switch (rank){

            default:
            case 1:
                //20.418 Seconds
                //689 41 442
                parallelConfig = new ParallelSolverConfiguration(
                        ParallelSolverConfiguration.SplittingStyle.LINEAR_AND_BINARY_SPLITTING_STYLE,
                        ParallelSolverConfiguration.SplittingObjectType.EVENT_SPLITTING_OBJECTS,
                        ParallelSolverConfiguration.SplittingObjectFilter.MUTUALLY_EXCLUSIVE_SO_FILTER,
                        ParallelSolverConfiguration.SplittingObjectSelection.SEEDED_RANDOM_SELECTION,

                        ParallelSolverConfiguration.FormulaGenerator.IN_SOLVER,
                        ParallelSolverConfiguration.ClauseSharingFilter.NO_CS_FILTER,
                        ParallelSolverConfiguration.ClauseReceivingFilter.NO_CR_FILTER,
                        2,
                        2,
                        4,
                        -861449674903621944L);
                break;
                case 2:
                //22.836 Seconds, 23.376 Seconds, 23.346 Seconds
                //602 20 437
                parallelConfig = new ParallelSolverConfiguration(
                        ParallelSolverConfiguration.SplittingStyle.LINEAR_AND_BINARY_SPLITTING_STYLE,
                        ParallelSolverConfiguration.SplittingObjectType.EVENT_SPLITTING_OBJECTS,
                        ParallelSolverConfiguration.SplittingObjectFilter.MUTUALLY_EXCLUSIVE_SO_FILTER,
                        ParallelSolverConfiguration.SplittingObjectSelection.SEEDED_RANDOM_SELECTION,
                        ParallelSolverConfiguration.FormulaGenerator.IN_SOLVER,
                        ParallelSolverConfiguration.ClauseSharingFilter.NO_CS_FILTER,
                        ParallelSolverConfiguration.ClauseReceivingFilter.NO_CR_FILTER,
                        2,
                        2,
                        4,
                        -8235967333839908600L);
                break;
            case 3:
                //24.138 Seconds
                //443 673 324
                parallelConfig = new ParallelSolverConfiguration(
                        ParallelSolverConfiguration.SplittingStyle.LINEAR_AND_BINARY_SPLITTING_STYLE,
                        ParallelSolverConfiguration.SplittingObjectType.EVENT_SPLITTING_OBJECTS,
                        ParallelSolverConfiguration.SplittingObjectFilter.MUTUALLY_EXCLUSIVE_SO_FILTER,
                        ParallelSolverConfiguration.SplittingObjectSelection.SEEDED_RANDOM_SELECTION,
                        ParallelSolverConfiguration.FormulaGenerator.IN_SOLVER,
                        ParallelSolverConfiguration.ClauseSharingFilter.NO_CS_FILTER,
                        ParallelSolverConfiguration.ClauseReceivingFilter.NO_CR_FILTER,
                        2,
                        2,
                        4,
                        1018159053681756496L);
                break;

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
                //19.106, 20.477 Seconds, 21.569 Seconds
                //700 47 451
                parallelConfig = new ParallelSolverConfiguration(
                        ParallelSolverConfiguration.SplittingStyle.LINEAR_AND_BINARY_SPLITTING_STYLE,
                        ParallelSolverConfiguration.SplittingObjectType.EVENT_SPLITTING_OBJECTS,
                        ParallelSolverConfiguration.SplittingObjectFilter.MUTUALLY_EXCLUSIVE_SO_FILTER,
                        ParallelSolverConfiguration.SplittingObjectSelection.SEEDED_RANDOM_SELECTION,
                        ParallelSolverConfiguration.FormulaGenerator.IN_SOLVER,
                        ParallelSolverConfiguration.ClauseSharingFilter.NO_CS_FILTER,
                        ParallelSolverConfiguration.ClauseReceivingFilter.NO_CR_FILTER,
                        2,
                        2,
                        4,
                        -861449674903621944L);

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
                //30.730 Seconds
                //329 203 656
                return new ParallelSolverConfiguration(
                        ParallelSolverConfiguration.SplittingStyle.LINEAR_AND_BINARY_SPLITTING_STYLE,
                        ParallelSolverConfiguration.SplittingObjectType.EVENT_SPLITTING_OBJECTS,
                        ParallelSolverConfiguration.SplittingObjectFilter.NO_SO_FILTER,
                        ParallelSolverConfiguration.SplittingObjectSelection.SEEDED_RANDOM_SELECTION,

                        ParallelSolverConfiguration.FormulaGenerator.IN_SOLVER,
                        ParallelSolverConfiguration.ClauseSharingFilter.NO_CS_FILTER,
                        ParallelSolverConfiguration.ClauseReceivingFilter.IMPLIES_CR_FILTER,
                        2,
                        2,
                        4,
                        7062032903458513384L);




            case 2:
                //40.804 Seconds, 42.214 Seconds
                //118 747 355
                parallelConfig = new ParallelSolverConfiguration(
                        ParallelSolverConfiguration.SplittingStyle.LINEAR_AND_BINARY_SPLITTING_STYLE,
                        ParallelSolverConfiguration.SplittingObjectType.EVENT_SPLITTING_OBJECTS,
                        ParallelSolverConfiguration.SplittingObjectFilter.MUTUALLY_EXCLUSIVE_SO_FILTER,
                        ParallelSolverConfiguration.SplittingObjectSelection.SEEDED_RANDOM_SELECTION,
                        ParallelSolverConfiguration.FormulaGenerator.IN_SOLVER,
                        ParallelSolverConfiguration.ClauseSharingFilter.NO_CS_FILTER,
                        ParallelSolverConfiguration.ClauseReceivingFilter.NO_CR_FILTER,
                        2,
                        2,
                        4,
                        -8235967333839908600L);

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
                //36.949 Seconds
                //701 773 128
                parallelConfig = new ParallelSolverConfiguration(ParallelSolverConfiguration.SplittingStyle.LINEAR_AND_BINARY_SPLITTING_STYLE,
                        ParallelSolverConfiguration.SplittingObjectType.EVENT_SPLITTING_OBJECTS,
                        ParallelSolverConfiguration.SplittingObjectFilter.MUTUALLY_EXCLUSIVE_SO_FILTER,
                        ParallelSolverConfiguration.SplittingObjectSelection.SEEDED_RANDOM_SELECTION,
                        ParallelSolverConfiguration.FormulaGenerator.IN_SOLVER,
                        ParallelSolverConfiguration.ClauseSharingFilter.NO_CS_FILTER,
                        ParallelSolverConfiguration.ClauseReceivingFilter.NO_CR_FILTER,
                        2,
                        2,
                        4,
                        -5548740888560639255L);

        }}catch(Exception e){
            System.out.println("Unreachable code reached. Seed Leaderboard DLM-3 Tso Rank " + rank + " failed. Error Message:" + e.getMessage());
        }
        return parallelConfig;
    }
}

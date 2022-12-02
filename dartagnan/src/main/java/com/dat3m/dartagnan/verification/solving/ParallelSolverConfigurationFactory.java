package com.dat3m.dartagnan.verification.solving;

public class ParallelSolverConfigurationFactory {



    public static ParallelSolverConfiguration basicEventConfig(){
        return new ParallelSolverConfiguration(
                ParallelSolverConfiguration.SplittingStyle.BINARY_SPLITTING_STYLE,
                ParallelSolverConfiguration.SplittingObjectType.BRANCH_EVENTS_SPLITTING_OBJECTS,
                ParallelSolverConfiguration.SplittingObjectFilter.NO_SO_FILTER,
                ParallelSolverConfiguration.SplittingObjectSelection.RANDOM_SELECTION,
                ParallelSolverConfiguration.StaticProgramAnalysis.BASELINE_SPA,
                ParallelSolverConfiguration.FormulaGenerator.IN_SOLVER,
                ParallelSolverConfiguration.ClauseSharingFilter.NO_CS_FILTER,
                ParallelSolverConfiguration.ClauseReceivingFilter.NO_CR_FILTER,
                3,
                0,
                4,
                -861449674903621944L);
    }

    public static ParallelSolverConfiguration seededEventConfig(long chosenSeed) {
        return  (new ParallelSolverConfiguration(
                ParallelSolverConfiguration.SplittingStyle.BINARY_SPLITTING_STYLE,
                ParallelSolverConfiguration.SplittingObjectType.BRANCH_EVENTS_SPLITTING_OBJECTS,
                ParallelSolverConfiguration.SplittingObjectFilter.NO_SO_FILTER,
                ParallelSolverConfiguration.SplittingObjectSelection.SEEDED_RANDOM_SELECTION,
                ParallelSolverConfiguration.StaticProgramAnalysis.BASELINE_SPA,
                ParallelSolverConfiguration.FormulaGenerator.IN_SOLVER,
                ParallelSolverConfiguration.ClauseSharingFilter.NO_CS_FILTER,
                ParallelSolverConfiguration.ClauseReceivingFilter.NO_CR_FILTER,
                3,
                0,
                4,
                chosenSeed
        ));
    }

    public static ParallelSolverConfiguration chosenEventConfig(int[] chosenEvents){
        ParallelSolverConfiguration parallelConfig =  new ParallelSolverConfiguration(
                ParallelSolverConfiguration.SplittingStyle.BINARY_SPLITTING_STYLE,
                ParallelSolverConfiguration.SplittingObjectType.BRANCH_EVENTS_SPLITTING_OBJECTS,
                ParallelSolverConfiguration.SplittingObjectFilter.NO_SO_FILTER,
                ParallelSolverConfiguration.SplittingObjectSelection.CHOSEN_SELECTION,
                ParallelSolverConfiguration.StaticProgramAnalysis.BASELINE_SPA,
                ParallelSolverConfiguration.FormulaGenerator.IN_SOLVER,
                ParallelSolverConfiguration.ClauseSharingFilter.NO_CS_FILTER,
                ParallelSolverConfiguration.ClauseReceivingFilter.NO_CR_FILTER,
                chosenEvents.length,
                0,
                4,
                0L);
        parallelConfig.setChosenEvents(chosenEvents);
        return parallelConfig;
    }


    public static ParallelSolverConfiguration copyConfiguration(ParallelSolverConfiguration parallelConfig){
        return new ParallelSolverConfiguration(
                parallelConfig.getSplittingStyle(),
                parallelConfig.getSplittingObjectType(),
                parallelConfig.getSplittingObjectFilter(),
                parallelConfig.getSplittingObjectSelection(),
                parallelConfig.getStaticProgramAnalysis(),
                parallelConfig.getFormulaGenerator(),
                parallelConfig.getClauseSharingFilter(),
                parallelConfig.getClauseReceivingFilter(),
                parallelConfig.getQueueSettingIntN(),
                parallelConfig.getQueueSettingIntM(),
                parallelConfig.getMaxNumberOfConcurrentThreads(),
                parallelConfig.getRandomSeed()
        );
    }

    public static ParallelSolverConfiguration Dglm3TsoLeaderboard(int rank){
        ParallelSolverConfiguration parallelConfig = null;

        try{switch (rank){

            default:
            case 1:
                //20.418 Seconds
                //689 41 442
                parallelConfig = seededEventConfig(-861449674903621944L);
                break;
            case 2:
                //22.836 Seconds, 23.376 Seconds, 23.346 Seconds
                //602 20 437
                parallelConfig = seededEventConfig(-8235967333839908600L);
                break;
            case 3:
                //24.138 Seconds
                //443 673 324
                parallelConfig = seededEventConfig(1018159053681756496L);
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
                parallelConfig = seededEventConfig(-861449674903621944L);

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
                return seededEventConfig(7062032903458513384L);




            case 2:
                //40.804 Seconds, 42.214 Seconds
                //118 747 355
                parallelConfig = seededEventConfig(-8235967333839908600L);

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
                parallelConfig = seededEventConfig(-5548740888560639255L);

        }}catch(Exception e){
            System.out.println("Unreachable code reached. Seed Leaderboard DLM-3 Tso Rank " + rank + " failed. Error Message:" + e.getMessage());
        }
        return parallelConfig;
    }
}

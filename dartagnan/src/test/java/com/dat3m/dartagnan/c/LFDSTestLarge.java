package com.dat3m.dartagnan.c;

import com.dat3m.dartagnan.configuration.Arch;
import com.dat3m.dartagnan.utils.Result;
import com.dat3m.dartagnan.utils.rules.CSVLogger;
import com.dat3m.dartagnan.utils.rules.Provider;
import com.dat3m.dartagnan.verification.model.ExecutionModel;
import com.dat3m.dartagnan.verification.solving.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.java_smt.SolverContextFactory;

import java.io.IOException;
import java.util.Arrays;

import static com.dat3m.dartagnan.configuration.Arch.*;
import static com.dat3m.dartagnan.utils.ResourceHelper.TEST_RESOURCE_PATH;
import static com.dat3m.dartagnan.utils.Result.UNKNOWN;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class LFDSTestLarge extends AbstractCTest {

    public LFDSTestLarge(String name, Arch target, Result expected) {
        super(name, target, expected);
    }

    @Override
    protected Provider<String> getProgramPathProvider() {
        return Provider.fromSupplier(() -> TEST_RESOURCE_PATH + "lfds/" + name + ".bpl");
    }

    @Override
    protected long getTimeout() {
        return 600000;
    }

    protected Provider<Integer> getBoundProvider() {
        return Provider.fromSupplier(() -> 2);
    }

	@Parameterized.Parameters(name = "{index}: {0}, target={1}")
    public static Iterable<Object[]> data() throws IOException {
		return Arrays.asList(new Object[][]{
            //{"safe_stack-3", TSO, Result.FAIL},
            //{"safe_stack-3", ARM8, FAIL},
            //{"safe_stack-3", POWER, FAIL}, //Power ausschalten*/
            //{"dglm-3", TSO, UNKNOWN},
            //{"dglm-3", ARM8, UNKNOWN},
                //{"dglm-3", C11, UNKNOWN},

            //{"dglm-3", POWER, UNKNOWN},
            //{"ms-3", TSO, UNKNOWN},
            //{"ms-3", ARM8, UNKNOWN},
            //{"ms-3", POWER, UNKNOWN},
            {"treiber-3", TSO, UNKNOWN},
            //{"treiber-3", ARM8, UNKNOWN},
            //{"treiber-3", POWER, UNKNOWN},
        });
    }

	//@Test
	//@CSVLogger.FileName("csv/assume")
	public void testAssume() throws Exception {
        AssumeSolver s = AssumeSolver.run(contextProvider.get(), proverProvider.get(), taskProvider.get());
        assertEquals(expected, s.getResult());
	}

	//@Test
	//@CSVLogger.FileName("csv/refinement")
	public void testRefinement() throws Exception {
        RefinementSolver s = RefinementSolver.run(contextProvider.get(), proverProvider.get(), taskProvider.get());
        ExecutionModel ec = ExecutionModel.withContext(s.getEncodingContext());
        ec.initialize(proverProvider.get().getModel());



        assertEquals(expected, s.getResult());
	}


    @Test
    @CSVLogger.FileName("csv/parallelRefinement")
    public void testParallelRefinement0() throws Exception {
        int[] chosenIDs = {442, 678};
        ParallelSolverConfiguration parallelConfig = ParallelSolverConfigurationFactory.chosenEventConfig(chosenIDs);
        //ParallelSolverConfiguration parallelConfig = ParallelSolverConfigurationFactory.basicEventConfig();
        //parallelConfig.setSplittingObjectType(ParallelSolverConfiguration.SplittingObjectType.BRANCH_EVENTS_SPLITTING_OBJECTS);
        //parallelConfig.setSplittingObjectSelection(ParallelSolverConfiguration.SplittingObjectSelection.SCORE_SELECTION);
        //parallelConfig.setQueueSettingIntN(2);
        ParallelRefinementSolver s = ParallelRefinementSolver.run(contextProvider.get(), proverProvider.get(),
                taskProvider.get(), SolverContextFactory.Solvers.Z3, Configuration.defaultConfiguration(),
                shutdownManagerProvider.get(), parallelConfig);
                //SeedLeaderboard.Dglm3TsoLeaderboard(1));
        assertEquals(expected, s.getResult());
    }


    @Test
    @CSVLogger.FileName("csv/parallelRefinement")
    public void testParallelRefinement1() throws Exception {

        int[] chosenIDs = {442, 678};
        ParallelSolverConfiguration parallelConfig = ParallelSolverConfigurationFactory.chosenEventConfig(chosenIDs);
        //parallelConfig.setSplittingObjectType(ParallelSolverConfiguration.SplittingObjectType.BRANCH_EVENTS_SPLITTING_OBJECTS);
        //parallelConfig.setSplittingObjectSelection(ParallelSolverConfiguration.SplittingObjectSelection.SCORE_SELECTION);
        //parallelConfig.setQueueSettingIntN(2);

        ParallelRefinementSolver s = ParallelRefinementSolver.run(contextProvider.get(), proverProvider.get(), taskProvider.get(), SolverContextFactory.Solvers.Z3,
                Configuration.defaultConfiguration(), shutdownManagerProvider.get(),
                parallelConfig);
                //SeedLeaderboard.Dglm3Arm8Leaderboard(1));
        assertEquals(expected, s.getResult());
    }


    @Test
    @CSVLogger.FileName("csv/parallelRefinement")
    public void testParallelRefinement2() throws Exception {
        int[] chosenIDs = {442, 678};
        ParallelSolverConfiguration parallelConfig = ParallelSolverConfigurationFactory.chosenEventConfig(chosenIDs);
        //parallelConfig.setSplittingObjectType(ParallelSolverConfiguration.SplittingObjectType.BRANCH_EVENTS_SPLITTING_OBJECTS);
        //parallelConfig.setSplittingObjectSelection(ParallelSolverConfiguration.SplittingObjectSelection.SCORE_SELECTION);
        //parallelConfig.setQueueSettingIntN(2);
        ParallelRefinementSolver s = ParallelRefinementSolver.run(contextProvider.get(), proverProvider.get(),
                taskProvider.get(), SolverContextFactory.Solvers.Z3, Configuration.defaultConfiguration(),
                shutdownManagerProvider.get(), parallelConfig);
                //SeedLeaderboard.Ms3TsoLeaderboard(1));
        assertEquals(expected, s.getResult());
    }

    @Test
    @CSVLogger.FileName("csv/parallelRefinement")
    public void testParallelRefinement3() throws Exception {
        int[] chosenIDs = {442, 678};
        ParallelSolverConfiguration parallelConfig = ParallelSolverConfigurationFactory.chosenEventConfig(chosenIDs);
        //parallelConfig.setSplittingObjectType(ParallelSolverConfiguration.SplittingObjectType.BRANCH_EVENTS_SPLITTING_OBJECTS);
        //parallelConfig.setSplittingObjectSelection(ParallelSolverConfiguration.SplittingObjectSelection.SCORE_SELECTION);
        //parallelConfig.setQueueSettingIntN(2);
        ParallelRefinementSolver s = ParallelRefinementSolver.run(contextProvider.get(), proverProvider.get(),
                taskProvider.get(), SolverContextFactory.Solvers.Z3,
                Configuration.defaultConfiguration(), shutdownManagerProvider.get(),
                parallelConfig);
                //SeedLeaderboard.Ms3Arm8Leaderboard(1));
        assertEquals(expected, s.getResult());
    }




    //@Test
    //@CSVLogger.FileName("csv/assume")
    public void testParallelAssume() throws Exception {

        ParallelAssumeSolver s = ParallelAssumeSolver.run(contextProvider.get(), proverProvider.get(),
                taskProvider.get(), SolverContextFactory.Solvers.Z3, Configuration.defaultConfiguration(),
                shutdownManagerProvider.get(), ParallelSolverConfigurationFactory.randomEventConfig());
        assertEquals(expected, s.getResult());
    }


}
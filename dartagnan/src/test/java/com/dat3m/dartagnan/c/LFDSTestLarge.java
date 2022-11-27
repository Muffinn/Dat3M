package com.dat3m.dartagnan.c;

import com.dat3m.dartagnan.configuration.Arch;
import com.dat3m.dartagnan.utils.Result;
import com.dat3m.dartagnan.utils.rules.CSVLogger;
import com.dat3m.dartagnan.utils.rules.Provider;
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
            //{"safe_stack-3", TSO, FAIL},
            //{"safe_stack-3", ARM8, FAIL},
            //{"safe_stack-3", POWER, FAIL}, //Power ausschalten*/
            {"dglm-3", TSO, UNKNOWN},
            //{"dglm-3", ARM8, UNKNOWN},
            //{"dglm-3", POWER, UNKNOWN},
            //{"ms-3", TSO, UNKNOWN},
            //{"ms-3", ARM8, UNKNOWN},
            //{"ms-3", POWER, UNKNOWN},
            //{"treiber-3", TSO, UNKNOWN},
            //{"treiber-3", ARM8, UNKNOWN},
            //{"treiber-3", POWER, UNKNOWN},
        });
    }

	@Test
	@CSVLogger.FileName("csv/assume")
	public void testAssume() throws Exception {
        AssumeSolver s = AssumeSolver.run(contextProvider.get(), proverProvider.get(), taskProvider.get());
        assertEquals(expected, s.getResult());
	}

	//@Test
	//@CSVLogger.FileName("csv/refinement")
	public void testRefinement() throws Exception {
        RefinementSolver s = RefinementSolver.run(contextProvider.get(), proverProvider.get(), taskProvider.get());
        assertEquals(expected, s.getResult());
	}


    //@Test
    @CSVLogger.FileName("csv/parallelRefinement")
    public void testParallelRefinement0() throws Exception {
        ParallelSolverConfiguration parallelConfig = new ParallelSolverConfiguration(
                ParallelSolverConfiguration.SplittingStyle.BINARY_SPLITTING_STYLE,
                ParallelSolverConfiguration.SplittingObjectType.EVENT_SPLITTING_OBJECTS,
                ParallelSolverConfiguration.SplittingObjectFilter.NO_SO_FILTER,
                ParallelSolverConfiguration.SplittingObjectSelection.CHOSEN_SELECTION,
                ParallelSolverConfiguration.StaticProgramAnalysis.BASELINE_SPA,
                ParallelSolverConfiguration.FormulaGenerator.IN_SOLVER,
                ParallelSolverConfiguration.ClauseSharingFilter.NO_CS_FILTER,
                ParallelSolverConfiguration.ClauseReceivingFilter.NO_CR_FILTER,
                2,
                2,
                4,
                -861449674903621944L);
        int[] chosenIDs = {442, 678};
        parallelConfig.setChosenEvents(chosenIDs);
        ParallelRefinementSolver s = ParallelRefinementSolver.run(contextProvider.get(), proverProvider.get(), taskProvider.get(), SolverContextFactory.Solvers.Z3,
                Configuration.defaultConfiguration(), shutdownManagerProvider.get(),
                parallelConfig);
                //SeedLeaderboard.Dglm3TsoLeaderboard(1));
        assertEquals(expected, s.getResult());
    }


    //@Test
    @CSVLogger.FileName("csv/parallelRefinement")
    public void testParallelRefinement1() throws Exception {
        ParallelSolverConfiguration parallelConfig = new ParallelSolverConfiguration(
                ParallelSolverConfiguration.SplittingStyle.BINARY_SPLITTING_STYLE,
                ParallelSolverConfiguration.SplittingObjectType.EVENT_SPLITTING_OBJECTS,
                ParallelSolverConfiguration.SplittingObjectFilter.NO_SO_FILTER,
                ParallelSolverConfiguration.SplittingObjectSelection.CHOSEN_SELECTION,
                ParallelSolverConfiguration.StaticProgramAnalysis.BASELINE_SPA,
                ParallelSolverConfiguration.FormulaGenerator.IN_SOLVER,
                ParallelSolverConfiguration.ClauseSharingFilter.NO_CS_FILTER,
                ParallelSolverConfiguration.ClauseReceivingFilter.NO_CR_FILTER,
                2,
                2,
                4,
                -861449674903621944L);
        int[] chosenIDs = {442, 678};
        parallelConfig.setChosenEvents(chosenIDs);
        ParallelRefinementSolver s = ParallelRefinementSolver.run(contextProvider.get(), proverProvider.get(), taskProvider.get(), SolverContextFactory.Solvers.Z3,
                Configuration.defaultConfiguration(), shutdownManagerProvider.get(),
                parallelConfig);
                //SeedLeaderboard.Dglm3Arm8Leaderboard(1));
        assertEquals(expected, s.getResult());
    }


    //@Test
    @CSVLogger.FileName("csv/parallelRefinement")
    public void testParallelRefinement2() throws Exception {
        ParallelSolverConfiguration parallelConfig = new ParallelSolverConfiguration(
                ParallelSolverConfiguration.SplittingStyle.BINARY_SPLITTING_STYLE,
                ParallelSolverConfiguration.SplittingObjectType.EVENT_SPLITTING_OBJECTS,
                ParallelSolverConfiguration.SplittingObjectFilter.NO_SO_FILTER,
                ParallelSolverConfiguration.SplittingObjectSelection.CHOSEN_SELECTION,
                ParallelSolverConfiguration.StaticProgramAnalysis.BASELINE_SPA,
                ParallelSolverConfiguration.FormulaGenerator.IN_SOLVER,
                ParallelSolverConfiguration.ClauseSharingFilter.NO_CS_FILTER,
                ParallelSolverConfiguration.ClauseReceivingFilter.NO_CR_FILTER,
                2,
                2,
                4,
                -861449674903621944L);
        int[] chosenIDs = {442, 689};
        parallelConfig.setChosenEvents(chosenIDs);
        ParallelRefinementSolver s = ParallelRefinementSolver.run(contextProvider.get(), proverProvider.get(), taskProvider.get(), SolverContextFactory.Solvers.Z3,
                Configuration.defaultConfiguration(), shutdownManagerProvider.get(),
                parallelConfig);
                //SeedLeaderboard.Ms3TsoLeaderboard(1));
        assertEquals(expected, s.getResult());
    }

    //@Test
    @CSVLogger.FileName("csv/parallelRefinement")
    public void testParallelRefinement3() throws Exception {
        ParallelSolverConfiguration parallelConfig = new ParallelSolverConfiguration(
                ParallelSolverConfiguration.SplittingStyle.BINARY_SPLITTING_STYLE,
                ParallelSolverConfiguration.SplittingObjectType.EVENT_SPLITTING_OBJECTS,
                ParallelSolverConfiguration.SplittingObjectFilter.NO_SO_FILTER,
                ParallelSolverConfiguration.SplittingObjectSelection.CHOSEN_SELECTION,
                ParallelSolverConfiguration.StaticProgramAnalysis.BASELINE_SPA,
                ParallelSolverConfiguration.FormulaGenerator.IN_SOLVER,
                ParallelSolverConfiguration.ClauseSharingFilter.NO_CS_FILTER,
                ParallelSolverConfiguration.ClauseReceivingFilter.NO_CR_FILTER,
                2,
                2,
                4,
                -861449674903621944L);
        int[] chosenIDs = {689, 442};
        parallelConfig.setChosenEvents(chosenIDs);
        ParallelRefinementSolver s = ParallelRefinementSolver.run(contextProvider.get(), proverProvider.get(), taskProvider.get(), SolverContextFactory.Solvers.Z3,
                Configuration.defaultConfiguration(), shutdownManagerProvider.get(),
                parallelConfig);
                //SeedLeaderboard.Ms3Arm8Leaderboard(1));
        assertEquals(expected, s.getResult());
    }




    @Test
    //@CSVLogger.FileName("csv/assume")
    public void testParallelAssume() throws Exception {

        ParallelAssumeSolver s = ParallelAssumeSolver.run(contextProvider.get(), proverProvider.get(), taskProvider.get(), SolverContextFactory.Solvers.Z3,
        Configuration.defaultConfiguration(), shutdownManagerProvider.get(),
                ParallelSolverConfigurationFactory.basicEventConfig());
        assertEquals(expected, s.getResult());
    }


}
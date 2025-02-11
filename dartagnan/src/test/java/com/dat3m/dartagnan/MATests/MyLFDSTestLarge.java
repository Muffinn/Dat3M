package com.dat3m.dartagnan.MATests;

import com.dat3m.dartagnan.c.AbstractCTest;
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
public class MyLFDSTestLarge extends AbstractCTest {

    private String reportFileName;

    public MyLFDSTestLarge(String name, Arch target, Result expected, String reportFileName) {
        super(name, target, expected);
        this.reportFileName = reportFileName;
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
            {"safe_stack-3", TSO, Result.FAIL, "kaktus"},
                {"safe_stack-3", TSO, Result.FAIL, "kaktus"},
                {"safe_stack-3", TSO, Result.FAIL, "kaktus"},
                {"safe_stack-3", TSO, Result.FAIL, "kaktus"},
            //{"safe_stack-3", ARM8, Result.FAIL},
            //{"safe_stack-3", C11, Result.FAIL},
            //{"dglm-3", TSO, UNKNOWN, "kaktus"},
            //{"dglm-3", ARM8, UNKNOWN, "kaktus"},
            //{"dglm-3", C11, UNKNOWN, "kaktus"},
            //{"ms-3", TSO, UNKNOWN},
            //{"ms-3", ARM8, UNKNOWN},
            //{"ms-3", C11, UNKNOWN},
            //{"treiber-3", TSO, UNKNOWN, "bamboo"},
            //{"treiber-3", ARM8, UNKNOWN, "bamboo"},
            //{"treiber-3", C11, UNKNOWN, "bamboo"},
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
        ParallelSolverConfiguration parallelConfig = ParallelSolverConfigurationFactory.noSplittingConfig(4);
        parallelConfig.initializeFileReport(reportFileName, target.toString(), name, "PR");
        ParallelRefinementSolver s = ParallelRefinementSolver.run(contextProvider.get(), proverProvider.get(),
                taskProvider.get(), SolverContextFactory.Solvers.Z3, Configuration.defaultConfiguration(),
                shutdownManagerProvider.get(), parallelConfig);

                //SeedLeaderboard.Dglm3TsoLeaderboard(1));
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
package com.dat3m.dartagnan.MATests.basicTests;

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
import java.util.LinkedList;

import static com.dat3m.dartagnan.configuration.Arch.*;
import static com.dat3m.dartagnan.utils.ResourceHelper.TEST_RESOURCE_PATH;
import static com.dat3m.dartagnan.utils.Result.UNKNOWN;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class NoSplittingNoSharingTest extends AbstractCTest {

    private String reportFileName;
    private int nrThreads;

    public NoSplittingNoSharingTest(String name, Arch target, Result expected, String reportFileName, int nrThreads) {
        super(name, target, expected);
        this.reportFileName = reportFileName;
        this.nrThreads = nrThreads;
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
		Object[] jitDelete = new Object[]{"treiber-3", C11, UNKNOWN, "JIT_Delete", 1};
        LinkedList<Object[]> objectList = new LinkedList<Object[]>();
        objectList.add(jitDelete);
        for(int i = 1; i < 6; i++){
            for (int j = 0; j < 10; j++) {
                Object[][] parameterArray = new Object[][]{
                        //{"safe_stack-3", TSO, Result.FAIL},
                        //{"safe_stack-3", ARM8, Result.FAIL},
                        //{"safe_stack-3", C11, Result.FAIL},
                        {"dglm-3", TSO, UNKNOWN, "noSplittingNoSharing", i},
                        {"dglm-3", ARM8, UNKNOWN, "noSplittingNoSharing", i},
                        {"dglm-3", C11, UNKNOWN, "noSplittingNoSharing", i},
                        {"ms-3", TSO, UNKNOWN, "noSplittingNoSharing", i},
                        {"ms-3", ARM8, UNKNOWN, "noSplittingNoSharing", i},
                        {"ms-3", C11, UNKNOWN, "noSplittingNoSharing", i},
                        {"treiber-3", TSO, UNKNOWN, "noSplittingNoSharing", i},
                        {"treiber-3", ARM8, UNKNOWN, "noSplittingNoSharing", i},
                        {"treiber-3", C11, UNKNOWN, "noSplittingNoSharing", i},
                };
                objectList.addAll(Arrays.asList(parameterArray));
            }

        }
        return objectList;
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
    public void testParallelRefinement() throws Exception {
        ParallelSolverConfiguration parallelConfig = ParallelSolverConfigurationFactory.noSplittingConfig(nrThreads);
        parallelConfig.setClauseSharingFilter(ParallelSolverConfiguration.ClauseSharingFilter.NO_CLAUSE_SHARING);
        parallelConfig.initializeFileReport(reportFileName, target.toString(), name, "PR");

        ParallelRefinementSolver s = ParallelRefinementSolver.run(contextProvider.get(), proverProvider.get(),
                taskProvider.get(), SolverContextFactory.Solvers.Z3, Configuration.defaultConfiguration(),
                shutdownManagerProvider.get(), parallelConfig);
        assertEquals(expected, s.getResult());
    }


    //@Test
    //@CSVLogger.FileName("csv/assume")
    public void testParallelAssume() throws Exception {

        ParallelAssumeSolver s = ParallelAssumeSolver.run(contextProvider.get(), proverProvider.get(),
                taskProvider.get(), SolverContextFactory.Solvers.Z3, Configuration.defaultConfiguration(),
                shutdownManagerProvider.get(), ParallelSolverConfigurationFactory.basicEventConfig());
        assertEquals(expected, s.getResult());
    }


}
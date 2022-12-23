package com.dat3m.dartagnan.MATests.SplittingObjectTests;

import com.dat3m.dartagnan.c.AbstractCTest;
import com.dat3m.dartagnan.configuration.Arch;
import com.dat3m.dartagnan.utils.Result;
import com.dat3m.dartagnan.utils.rules.CSVLogger;
import com.dat3m.dartagnan.utils.rules.Provider;
import com.dat3m.dartagnan.verification.solving.ParallelAssumeSolver;
import com.dat3m.dartagnan.verification.solving.ParallelRefinementSolver;
import com.dat3m.dartagnan.verification.solving.ParallelSolverConfiguration;
import com.dat3m.dartagnan.verification.solving.ParallelSolverConfigurationFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.java_smt.SolverContextFactory;

import java.io.IOException;
import java.util.LinkedList;

import static com.dat3m.dartagnan.configuration.Arch.*;
import static com.dat3m.dartagnan.utils.ResourceHelper.TEST_RESOURCE_PATH;
import static com.dat3m.dartagnan.utils.Result.UNKNOWN;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class RandomTupleTest extends AbstractCTest {

    private String reportFileName;
    private long randomSeed;
    private ParallelSolverConfiguration.SplittingObjectFilter soFilter;

    public RandomTupleTest(String name, Arch target, Result expected, String reportFileName, ParallelSolverConfiguration.SplittingObjectFilter soFilter) {
        super(name, target, expected);
        this.reportFileName = reportFileName;
        this.soFilter = soFilter;
        this.randomSeed = -3477897202812263156L;

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
		Object[] jitDelete = new Object[]{"treiber-3", C11, UNKNOWN, "JIT_Delete", ParallelSolverConfiguration.SplittingObjectFilter.IMP_AND_ME_SO_FILTER};
        LinkedList<Object[]> objectList = new LinkedList<Object[]>();
        objectList.add(jitDelete);
        String testName = "RandomTupleTests";

        String[] programNames = {"dglm-3", "ms-3", "treiber-3"};
        Arch[] wmms = {TSO, ARM8, C11};
        ParallelSolverConfiguration.SplittingObjectFilter[] Filters = new ParallelSolverConfiguration.SplittingObjectFilter[]{
                //ParallelSolverConfiguration.SplittingObjectFilter.NO_SO_FILTER,
                ParallelSolverConfiguration.SplittingObjectFilter.MUTUALLY_EXCLUSIVE_SO_FILTER,
                //ParallelSolverConfiguration.SplittingObjectFilter.IMPLIES_SO_FILTER,
                //ParallelSolverConfiguration.SplittingObjectFilter.IMP_AND_ME_SO_FILTER
        };


        for(ParallelSolverConfiguration.SplittingObjectFilter filter : Filters){
            for (Arch wmm:wmms) {
                for (String programName : programNames) {
                    for (int j = 0; j < 1; j++) {
                        Object[] objects = new Object[]{programName, wmm, UNKNOWN, testName, filter};
                        objectList.add(objects);
                    }
                }
            }
        }
        return objectList;
    }



    @Test
    @CSVLogger.FileName("csv/parallelRefinement")
    public void testParallelRefinement() throws Exception {
        ParallelSolverConfiguration parallelConfig = ParallelSolverConfigurationFactory.seededEventConfig(randomSeed);
        parallelConfig.setSplittingObjectFilter(soFilter);
        parallelConfig.setSplittingObjectType(ParallelSolverConfiguration.SplittingObjectType.RF_RELATION_SPLITTING_OBJECTS);
        parallelConfig.setSplittingIntN(25);
        parallelConfig.setSplittingStyle(ParallelSolverConfiguration.SplittingStyle.LINEAR_SPLITTING_STYLE);


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
                shutdownManagerProvider.get(), ParallelSolverConfigurationFactory.randomEventConfig());
        assertEquals(expected, s.getResult());
    }


}
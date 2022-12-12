package com.dat3m.dartagnan.MATests.ThreadTests;

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
public class FourCoresTest extends AbstractCTest {

    private String reportFileName;
    private int[] nAndM;

    public FourCoresTest(String name, Arch target, Result expected, String reportFileName, int[] nAndM) {
        super(name, target, expected);
        this.reportFileName = reportFileName;
        this.nAndM = nAndM;
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
		Object[] jitDelete = new Object[]{"treiber-3", C11, UNKNOWN, "JIT_Delete", new int[]{1, 1}};
        LinkedList<Object[]> objectList = new LinkedList<Object[]>();
        objectList.add(jitDelete);
        String testName = "ThreadNRTesting";

        String[] programNames = {"dglm-3", "ms-3", "treiber-3"};
        Arch[] wmms = {TSO, ARM8, C11};


        int[][] nAndMArray = {{1,1}, {2,1}, {3,1}, {2,2}};
        for(int[] nAndM : nAndMArray){
            for (Arch wmm:wmms) {
                for (String programName : programNames) {
                    for (int j = 0; j < 10; j++) {
                        Object[] objects = new Object[]{programName, wmm, UNKNOWN, testName, nAndM};
                        objectList.add(objects);
                    }
                }
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
        ParallelSolverConfiguration parallelConfig = ParallelSolverConfigurationFactory.scoredEventConfig();
        parallelConfig.setSplittingStyle(ParallelSolverConfiguration.SplittingStyle.LINEAR_AND_BINARY_SPLITTING_STYLE);
        parallelConfig.setSplittingIntN(nAndM[0]);
        parallelConfig.setSplittingIntM(nAndM[1]);


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
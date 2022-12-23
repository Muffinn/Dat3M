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
import java.util.Random;

import static com.dat3m.dartagnan.configuration.Arch.*;
import static com.dat3m.dartagnan.utils.ResourceHelper.TEST_RESOURCE_PATH;
import static com.dat3m.dartagnan.utils.Result.UNKNOWN;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class MassRandomCOTest extends AbstractCTest {

    private String reportFileName;
    private int [] nAndM;
    private long seed;
    private ParallelSolverConfiguration.SplittingStyle splittingStyle;

    public MassRandomCOTest(String name, Arch target, Result expected, String reportFileName, ParallelSolverConfiguration.SplittingStyle splittingStyle , int[] nAndM, long seed) {
        super(name, target, expected);
        this.reportFileName = reportFileName;
        this.nAndM = nAndM;
        this.seed = seed;
        this.splittingStyle = splittingStyle;

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
        long seeed = new Random().nextLong();
        Object[] jitDelete = new Object[]{"treiber-3", C11, UNKNOWN, "JIT_Delete", ParallelSolverConfiguration.SplittingStyle.BINARY_SPLITTING_STYLE, new int[]{1,1}, seeed};
        LinkedList<Object[]> objectList = new LinkedList<Object[]>();
        objectList.add(jitDelete);
        String testName = "MassRandomCO";

        String[] programNames = {"dglm-3", "ms-3", "treiber-3"};
        Arch[] wmms = {TSO, ARM8, C11};
        int[][] nAndMArray = {{8,0}, {3,0}, {4,1}};
        ParallelSolverConfiguration.SplittingStyle[] splittingStyles = new ParallelSolverConfiguration.SplittingStyle[]{
                ParallelSolverConfiguration.SplittingStyle.LINEAR_SPLITTING_STYLE,
                ParallelSolverConfiguration.SplittingStyle.BINARY_SPLITTING_STYLE,
                ParallelSolverConfiguration.SplittingStyle.LINEAR_AND_BINARY_SPLITTING_STYLE
        };


        //for (ParallelSolverConfiguration.SplittingStyle splittingStyle :splittingStyles) {
            for (int i = 0; i < 2; i++) {
                seeed = new Random().nextLong();
                for (Arch wmm : wmms) {
                    for (String programName : programNames) {
                        for (int j = 0; j < 3; j++) {
                            Object[] objects = new Object[]{programName, wmm, UNKNOWN, testName, splittingStyles[j], nAndMArray[j], seeed};
                            objectList.add(objects);
                        }
                    }
                }
            }
        //}
        return objectList;
    }



    @Test
    @CSVLogger.FileName("csv/parallelRefinement")
    public void testParallelRefinement() throws Exception {
        ParallelSolverConfiguration parallelConfig = ParallelSolverConfigurationFactory.seededCOTupleConfig(seed);
        parallelConfig.setSplittingStyle(splittingStyle);
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
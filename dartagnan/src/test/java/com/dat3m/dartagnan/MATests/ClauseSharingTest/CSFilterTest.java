package com.dat3m.dartagnan.MATests.ClauseSharingTest;

import com.dat3m.dartagnan.c.AbstractCTest;
import com.dat3m.dartagnan.configuration.Arch;
import com.dat3m.dartagnan.utils.Result;
import com.dat3m.dartagnan.utils.rules.CSVLogger;
import com.dat3m.dartagnan.utils.rules.Provider;
import com.dat3m.dartagnan.verification.model.ExecutionModel;
import com.dat3m.dartagnan.verification.solving.*;
import de.uni_freiburg.informatik.ultimate.smtinterpol.dpll.Clause;
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
public class CSFilterTest extends AbstractCTest {

    private String reportFileName;
    private int[] nAndM;
    private ParallelSolverConfiguration.ClauseSharingFilter csFil;

    public CSFilterTest(String name, Arch target, Result expected, String reportFileName, int[] nAndM, ParallelSolverConfiguration.ClauseSharingFilter csFil) {
        super(name, target, expected);
        this.reportFileName = reportFileName;
        this.nAndM = nAndM;
        this.csFil = csFil;
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
		Object[] jitDelete = new Object[]{"treiber-3", C11, UNKNOWN, "JIT_Delete", new int[]{1, 1}, ParallelSolverConfiguration.ClauseSharingFilter.DUPLICATE_CS_FILTER};
        LinkedList<Object[]> objectList = new LinkedList<Object[]>();
        objectList.add(jitDelete);
        String testName = "ClauseSharingFilterTest";

        String[] programNames = {"dglm-3", "ms-3", "treiber-3"};
        Arch[] wmms = {TSO, ARM8, C11};
        ParallelSolverConfiguration.ClauseSharingFilter[] csFilter = {ParallelSolverConfiguration.ClauseSharingFilter.NO_CS_FILTER,
                ParallelSolverConfiguration.ClauseSharingFilter.DUPLICATE_CS_FILTER};


        int[][] nAndMArray = {{2,0}, {3,0}};
        for(int[] nAndM : nAndMArray){
            for (Arch wmm:wmms) {
                for (String programName : programNames) {
                    for(ParallelSolverConfiguration.ClauseSharingFilter csFil : csFilter) {
                        for (int j = 0; j < 1; j++) {
                            Object[] objects = new Object[]{programName, wmm, UNKNOWN, testName, nAndM, csFil};
                            objectList.add(objects);
                        }
                    }
                }
            }
        }
        return objectList;
    }



    @Test
    @CSVLogger.FileName("csv/parallelRefinement")
    public void testParallelRefinement() throws Exception {
        ParallelSolverConfiguration parallelConfig = ParallelSolverConfigurationFactory.scoredEventConfig();
        parallelConfig.setSplittingStyle(ParallelSolverConfiguration.SplittingStyle.BINARY_SPLITTING_STYLE);
        parallelConfig.setSplittingIntN(nAndM[0]);
        parallelConfig.setSplittingIntM(nAndM[1]);
        parallelConfig.setClauseSharingFilter(csFil);

        parallelConfig.initializeFileReport(reportFileName, target.toString(), name, "PR");

        ParallelRefinementSolver s = ParallelRefinementSolver.run(contextProvider.get(), proverProvider.get(),
                taskProvider.get(), SolverContextFactory.Solvers.Z3, Configuration.defaultConfiguration(),
                shutdownManagerProvider.get(), parallelConfig);
        assertEquals(expected, s.getResult());
    }





}
package com.dat3m.dartagnan.c;

import com.dat3m.dartagnan.configuration.Arch;
import com.dat3m.dartagnan.utils.Result;
import com.dat3m.dartagnan.utils.rules.CSVLogger;
import com.dat3m.dartagnan.utils.rules.Provider;
import com.dat3m.dartagnan.verification.solving.AssumeSolver;
import com.dat3m.dartagnan.verification.solving.ParallelAssumeSolver;
import com.dat3m.dartagnan.verification.solving.RefinementSolver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.java_smt.SolverContextFactory;

import java.io.IOException;
import java.util.Arrays;

import static com.dat3m.dartagnan.configuration.Arch.*;
import static com.dat3m.dartagnan.utils.ResourceHelper.TEST_RESOURCE_PATH;
import static com.dat3m.dartagnan.utils.Result.FAIL;
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
            {"dglm-3", ARM8, UNKNOWN},
            //{"dglm-3", POWER, UNKNOWN},
            {"ms-3", TSO, UNKNOWN},
            {"ms-3", ARM8, UNKNOWN},
            //{"ms-3", POWER, UNKNOWN},
            {"treiber-3", TSO, UNKNOWN},
            {"treiber-3", ARM8, UNKNOWN},
            //{"treiber-3", POWER, UNKNOWN},
        });
    }

	//@Test
	//@CSVLogger.FileName("csv/assume")
	public void testAssume() throws Exception {
		assertEquals(expected, AssumeSolver.run(contextProvider.get(), proverProvider.get(), taskProvider.get()));
	}

	//@Test
	@CSVLogger.FileName("csv/refinement")
	public void testRefinement() throws Exception {
		assertEquals(expected, RefinementSolver.run(contextProvider.get(), proverProvider.get(), taskProvider.get()));
	}

    @Test
    //@CSVLogger.FileName("csv/assume")
    public void testParallelAssumeSORT() throws Exception {
        assertEquals(expected, ParallelAssumeSolver.run(contextProvider.get(), proverProvider.get(), taskProvider.get(), SolverContextFactory.Solvers.MATHSAT5, shutdownManagerProvider.get(),
        Configuration.defaultConfiguration(), ParallelAssumeSolver.QueueType.MUTUALLY_EXCLUSIVE_EVENTS, 8,3 , 4));
    }

    /*@Test
    //@CSVLogger.FileName("csv/assume")
    public void testParallelAssumeMESORT() throws Exception {
        assertEquals(expected, ParallelAssumeSolver.run(contextProvider.get(), proverProvider.get(), taskProvider.get(), SolverContextFactory.Solvers.Z3, shutdownManagerProvider.get(),
                Configuration.defaultConfiguration(), ParallelAssumeSolver.QueueType.MUTUALLY_EXCLUSIVE_SORT, 8, 3, 1));
    }
    //MATHSAT5
    @Test
    //@CSVLogger.FileName("csv/assume")
    public void testParallelAssumeMESHUFFLE() throws Exception {
        assertEquals(expected, ParallelAssumeSolver.run(contextProvider.get(), proverProvider.get(), taskProvider.get(), SolverContextFactory.Solvers.Z3, shutdownManagerProvider.get(),
                Configuration.defaultConfiguration(), ParallelAssumeSolver.QueueType.MUTUALLY_EXCLUSIVE_SHUFFLE, 8, 3, 1));
    }

    @Test
    //@CSVLogger.FileName("csv/assume")
    public void testParallelAssumeEVENTS() throws Exception {
        assertEquals(expected, ParallelAssumeSolver.run(contextProvider.get(), proverProvider.get(), taskProvider.get(), SolverContextFactory.Solvers.Z3, shutdownManagerProvider.get(),
                Configuration.defaultConfiguration(), ParallelAssumeSolver.QueueType.EVENTS, 8, 3, 1));
    }

    @Test
    //@CSVLogger.FileName("csv/assume")
    public void testParallelAssumeMEEVENTS() throws Exception {
        assertEquals(expected, ParallelAssumeSolver.run(contextProvider.get(), proverProvider.get(), taskProvider.get(), SolverContextFactory.Solvers.Z3, shutdownManagerProvider.get(),
                Configuration.defaultConfiguration(), ParallelAssumeSolver.QueueType.MUTUALLY_EXCLUSIVE_EVENTS, 8, 3, 1));
    }

    @Test
    //@CSVLogger.FileName("csv/assume")
    public void testParallelAssumeSHUFFLE() throws Exception {
        assertEquals(expected, ParallelAssumeSolver.run(contextProvider.get(), proverProvider.get(), taskProvider.get(), SolverContextFactory.Solvers.Z3, shutdownManagerProvider.get(),
                Configuration.defaultConfiguration(), ParallelAssumeSolver.QueueType.RELATIONS_SHUFFLE, 8, 3, 1));
    }*/

}
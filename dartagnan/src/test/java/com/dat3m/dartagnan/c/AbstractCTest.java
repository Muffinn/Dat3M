package com.dat3m.dartagnan.c;

import com.dat3m.dartagnan.program.Program;
import com.dat3m.dartagnan.utils.Result;
import com.dat3m.dartagnan.utils.rules.CSVLogger;
import com.dat3m.dartagnan.utils.rules.Provider;
import com.dat3m.dartagnan.utils.rules.Providers;
import com.dat3m.dartagnan.utils.rules.RequestShutdownOnError;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.wmm.Wmm;
import com.dat3m.dartagnan.configuration.Arch;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.rules.Timeout;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext;

public abstract class AbstractCTest {

    protected String name;
    protected Arch target;
    protected Result expected;

    public AbstractCTest(String name, Arch target, Result expected) {
        this.name = name;
        this.target = target;
        this.expected = expected;
    }

    // =================== Modifiable behavior ====================

    protected abstract Provider<String> getProgramPathProvider();
    protected abstract long getTimeout();

    protected Provider<Integer> getBoundProvider() {
        return Provider.fromSupplier(() -> 1);
    }

    protected Provider<Integer> getTimeoutProvider() {
        return Provider.fromSupplier(() -> 0);
    }

    // =============================================================


    @ClassRule
    public static CSVLogger.Initialization csvInit = CSVLogger.Initialization.create();

    // Provider rules
    protected final Provider<ShutdownManager> shutdownManagerProvider = Provider.fromSupplier(ShutdownManager::create);
    protected final Provider<Arch> targetProvider = () -> target;
    protected final Provider<String> filePathProvider = getProgramPathProvider();
    protected final Provider<Integer> boundProvider = getBoundProvider();
    protected final Provider<Integer> timeoutProvider = getTimeoutProvider();
    protected final Provider<Program> programProvider = Providers.createProgramFromPath(filePathProvider);
    protected final Provider<Wmm> wmmProvider = Providers.createWmmFromArch(targetProvider);
    protected final Provider<VerificationTask> taskProvider = Providers.createTask(programProvider, wmmProvider, targetProvider, boundProvider, timeoutProvider);
    protected final Provider<SolverContext> contextProvider = Providers.createSolverContextFromManager(shutdownManagerProvider);
    protected final Provider<ProverEnvironment> proverProvider = Providers.createProverWithFixedOptions(contextProvider, SolverContext.ProverOptions.GENERATE_MODELS);

    // Special rules
    protected final Timeout timeout = Timeout.millis(getTimeout());
    protected final CSVLogger csvLogger = CSVLogger.create(() -> name, () -> expected);
    protected final RequestShutdownOnError shutdownOnError = RequestShutdownOnError.create(shutdownManagerProvider);

    @Rule
    public RuleChain ruleChain = RuleChain.outerRule(shutdownManagerProvider)
            .around(shutdownOnError)
            .around(filePathProvider)
            .around(boundProvider)
            .around(timeoutProvider)
            .around(programProvider)
            .around(wmmProvider)
            .around(taskProvider)
            .around(csvLogger)
            .around(timeout)
            // Context/Prover need to be created inside test-thread spawned by <timeout>
            .around(contextProvider)
            .around(proverProvider);

}

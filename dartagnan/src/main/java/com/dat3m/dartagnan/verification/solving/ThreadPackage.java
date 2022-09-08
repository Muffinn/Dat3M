package com.dat3m.dartagnan.verification.solving;

import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext;

public class ThreadPackage {
    private SolverContext solverContext;
    private ProverEnvironment proverEnvironment;
    private BooleanFormula propertyEncoding ;

    public ThreadPackage(){}

    public ThreadPackage(SolverContext solverContext, ProverEnvironment proverEnvironment, BooleanFormula propertyEncoding){
        this.solverContext = solverContext;
        this.proverEnvironment = proverEnvironment;
        this.propertyEncoding = propertyEncoding;
    }

    public SolverContext getSolverContext() {
        return solverContext;
    }

    public void setSolverContext(SolverContext solverContext) {
        this.solverContext = solverContext;
    }

    public ProverEnvironment getProverEnvironment() {
        return proverEnvironment;
    }

    public void setProverEnvironment(ProverEnvironment proverEnvironment) {
        this.proverEnvironment = proverEnvironment;
    }

    public BooleanFormula getPropertyEncoding() {
        return propertyEncoding;
    }

    public void setPropertyEncoding(BooleanFormula propertyEncoding) {
        this.propertyEncoding = propertyEncoding;
    }
}

package com.dat3m.dartagnan.verification.solving;

import org.sosy_lab.java_smt.api.BooleanFormula;

public class FormulaContainer {
    private BooleanFormula fullProgramFormula;
    private BooleanFormula wmmFormula;
    private BooleanFormula witnessFormula;
    private BooleanFormula symmFormula;

    FormulaContainer(){}

    public BooleanFormula getFullProgramFormula() {
        return fullProgramFormula;
    }

    public void setFullProgramFormula(BooleanFormula fullProgramFormula) {
        this.fullProgramFormula = fullProgramFormula;
    }

    public BooleanFormula getWmmFormula() {
        return wmmFormula;
    }

    public void setWmmFormula(BooleanFormula wmmFormula) {
        this.wmmFormula = wmmFormula;
    }

    public BooleanFormula getWitnessFormula() {
        return witnessFormula;
    }

    public void setWitnessFormula(BooleanFormula witnessFormula) {
        this.witnessFormula = witnessFormula;
    }

    public BooleanFormula getSymmFormula() {
        return symmFormula;
    }

    public void setSymmFormula(BooleanFormula symmFormula) {
        this.symmFormula = symmFormula;
    }






}

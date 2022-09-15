package com.dat3m.dartagnan.verification.solving;

import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.wmm.relation.RelationNameRepository;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverContext;

import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.dat3m.dartagnan.wmm.relation.RelationNameRepository.*;

public class FormulaQueueManager {
    private Queue<BooleanFormula> formulaQueue;


    public FormulaQueueManager(){
        this.formulaQueue = new ConcurrentLinkedQueue<BooleanFormula>();
    }

    public BooleanFormula getNextFormula() {
        return formulaQueue.remove();
    }

    public void addFormula(BooleanFormula addedFormula){
        formulaQueue.add(addedFormula);
    }

    public void queueTupleFormulas(int maxNumberOfLiterals, int numberOfPositiveLiterals, List<Tuple> tupleList, SolverContext mainCtx, VerificationTask task, String relationName)
            throws IllegalArgumentException{
        if(maxNumberOfLiterals < numberOfPositiveLiterals){
            throw new IllegalArgumentException("more positive Literals than max number of literals");
        }
        BooleanFormulaManager bmgr = mainCtx.getFormulaManager().getBooleanFormulaManager();
        for (int i = 1; i < maxNumberOfLiterals; i++){
            BooleanFormula newFormula = bmgr.makeTrue();
            for (int j = 0; j < (i - numberOfPositiveLiterals);j++){
                BooleanFormula notVar = bmgr.not(task.getMemoryModel().getRelationRepository().getRelation(relationName).getSMTVar(tupleList.get(j) ,mainCtx));
                newFormula = bmgr.and(newFormula, notVar);
            }
            for (int k = (i - numberOfPositiveLiterals); k < i; k++){
                BooleanFormula var = task.getMemoryModel().getRelationRepository().getRelation(relationName).getSMTVar(tupleList.get(k) ,mainCtx);
                newFormula = bmgr.and(newFormula, var);
            }
        }
    }
}
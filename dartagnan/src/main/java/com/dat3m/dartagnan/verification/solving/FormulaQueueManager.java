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

    public int getQueuesize(){return formulaQueue.size();}

    /* public void TrashqueueRelationTuples(int maxLength, int numberOfPositiveLiterals, List<Tuple> tupleList, SolverContext mainCtx, VerificationTask task, String relationName)
            throws IllegalArgumentException{
        if(maxLength < numberOfPositiveLiterals){
            throw new IllegalArgumentException("more positive Literals than max number of literals");
        }
        BooleanFormulaManager bmgr = mainCtx.getFormulaManager().getBooleanFormulaManager();
        for (int i = 1; i < maxLength; i++){
            BooleanFormula newFormula = bmgr.makeTrue();
            for (int j = 0; j < (i - numberOfPositiveLiterals);j++){
                BooleanFormula notVar = bmgr.not(task.getMemoryModel().getRelationRepository().getRelation(relationName).getSMTVar(tupleList.get(j) ,mainCtx));
                newFormula = bmgr.and(newFormula, notVar);
            }
            for (int k = (i - numberOfPositiveLiterals); k < i; k++){
                BooleanFormula var = task.getMemoryModel().getRelationRepository().getRelation(relationName).getSMTVar(tupleList.get(k) ,mainCtx);
                newFormula = bmgr.and(newFormula, var);
            }
            addFormula(newFormula);
        }
    }*/

    public void relationTuples(int maxLength, int maxTrue, List<Tuple> tupleList, SolverContext mainCtx, VerificationTask task, String relationName)
        throws IllegalArgumentException{
        if(maxLength > tupleList.size()){
            throw new IllegalArgumentException("Tuplelist of size " + tupleList.size() + " contains to few items to fill Formula of size " + maxLength);
        }
        BooleanFormulaManager bmgr = mainCtx.getFormulaManager().getBooleanFormulaManager();
        BooleanFormula newFormula = bmgr.makeTrue();

        recursiveRelationTuples(newFormula,0, maxLength, 0, maxTrue, tupleList, mainCtx, bmgr, task, relationName);

    }

    private void recursiveRelationTuples(BooleanFormula currentFormula, int length, int maxLength, int anzTrue, int maxTrue, List<Tuple> tupleList, SolverContext mainCtx,
                                        BooleanFormulaManager bmgr, VerificationTask task, String relationName){

        BooleanFormula var = task.getMemoryModel().getRelationRepository().getRelation(relationName).getSMTVar(tupleList.get(length), mainCtx);
        BooleanFormula notVar = bmgr.not(var);
        var = bmgr.and(var, currentFormula);
        notVar = bmgr.and(notVar, currentFormula);


        if(!(anzTrue + 1 == maxTrue || length + 1 == maxLength)){
            recursiveRelationTuples(notVar, length + 1, maxLength, anzTrue, maxTrue, tupleList, mainCtx, bmgr, task, relationName);
            recursiveRelationTuples(var, length + 1, maxLength,anzTrue + 1, maxTrue, tupleList, mainCtx, bmgr, task, relationName);
        } else {
            addFormula(notVar);
            //System.out.println("Added Formula: " + notVar);
            //System.out.println("Length: " + (length + 1) + " AnzTrue: " + anzTrue);
            addFormula(var);
            //.out.println("Added Formula: " + var);
            //System.out.println("Length: " + (length + 1) + " AnzTrue: " + (anzTrue + 1));
        }
    }
}
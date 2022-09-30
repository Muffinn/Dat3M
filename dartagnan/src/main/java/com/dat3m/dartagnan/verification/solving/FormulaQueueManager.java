package com.dat3m.dartagnan.verification.solving;

import com.dat3m.dartagnan.program.analysis.ExecutionAnalysis;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverContext;

import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FormulaQueueManager {
    private Queue<BooleanFormula> formulaQueue;
    private SolverContext ctx;
    private BooleanFormulaManager bmgr;
    private VerificationTask task;


    public FormulaQueueManager(SolverContext ctx, VerificationTask task){
        this.formulaQueue = new ConcurrentLinkedQueue<BooleanFormula>();
        this.ctx = ctx;
        this.bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
        this.task = task;
    }

    public BooleanFormula getNextFormula() {
        return formulaQueue.remove();
    }

    public void addFormula(BooleanFormula addedFormula){
        formulaQueue.add(addedFormula);
    }

    public int getQueuesize(){return formulaQueue.size();}

    public void relationTuplesMutuallyExlusive(int maxLength, int maxTrue, List<Tuple> tupleList, String relationName){
        removeMutualExclusiveTuples(tupleList, maxLength);
        int newMaxLength = Math.min(tupleList.size(), maxLength);
        int newMaxTrue = Math.min(maxTrue, newMaxLength);
        relationTuples(newMaxLength, newMaxTrue, tupleList, relationName);
    }

    public void relationTuples(int maxLength, int maxTrue, List<Tuple> tupleList, String relationName)
        throws IllegalArgumentException{
        if(maxLength > tupleList.size()){
            throw new IllegalArgumentException("Tuplelist of size " + tupleList.size() + " contains to few items to fill Formula of size " + maxLength);
        }
        BooleanFormula newFormula = bmgr.makeTrue();

        recursiveRelationTuples(newFormula,0, maxLength, 0, maxTrue, tupleList, relationName);

    }

    private void recursiveRelationTuples(BooleanFormula currentFormula, int length, int maxLength, int anzTrue, int maxTrue, List<Tuple> tupleList, String relationName){

        BooleanFormula var = task.getMemoryModel().getRelationRepository().getRelation(relationName).getSMTVar(tupleList.get(length), ctx);
        BooleanFormula notVar = bmgr.not(var);
        var = bmgr.and(var, currentFormula);
        notVar = bmgr.and(notVar, currentFormula);


        if(!(anzTrue + 1 == maxTrue || length + 1 == maxLength)){
            recursiveRelationTuples(notVar, length + 1, maxLength, anzTrue, maxTrue, tupleList, relationName);
            recursiveRelationTuples(var, length + 1, maxLength,anzTrue + 1, maxTrue, tupleList, relationName);
        } else {
            addFormula(notVar);
            //System.out.println("Added Formula: " + notVar);
            //System.out.println("Length: " + (length + 1) + " AnzTrue: " + anzTrue);
            addFormula(var);
            //.out.println("Added Formula: " + var);
            //System.out.println("Length: " + (length + 1) + " AnzTrue: " + (anzTrue + 1));
        }
    }

    public void createTrues(int numberOfTrues){
        for (int i = 0; i < numberOfTrues; i++){
            addFormula(bmgr.makeTrue());
        }
    }

    public void mutuallyExclusiveEventsQueue(int maxLength, int maxTrue, List<Event> eventList){
        removeMutualExclusiveEvents(eventList, maxLength);
        int newMaxLength = Math.min(eventList.size(), maxLength);
        int newMaxTrue = Math.min(maxTrue, newMaxLength);
        eventsQueue(newMaxLength, newMaxTrue, eventList);
    }

    public void eventsQueue(int maxLength, int maxTrue, List<Event> eventList)
        throws IllegalArgumentException{
            if(maxLength > eventList.size()){
                throw new IllegalArgumentException("Eventlist of size " + eventList.size() + " contains to few items to fill Formula of size " + maxLength);
            }
            BooleanFormula newFormula = bmgr.makeTrue();

            recursiveEventsQueue(newFormula, 0, maxLength, 0, maxTrue, eventList);
    }

    private void recursiveEventsQueue(BooleanFormula currentFormula, int length, int maxLength, int anzTrue, int maxTrue, List<Event> eventList){

        BooleanFormula var = eventList.get(length).exec();
        BooleanFormula notVar = bmgr.not(var);
        var = bmgr.and(var, currentFormula);
        notVar = bmgr.and(notVar, currentFormula);


        if(!(anzTrue + 1 == maxTrue || length + 1 == maxLength)){
            recursiveEventsQueue(notVar, length + 1, maxLength, anzTrue, maxTrue, eventList);
            recursiveEventsQueue(var, length + 1, maxLength,anzTrue + 1, maxTrue, eventList);
        } else {
            addFormula(notVar);
            System.out.println("Added Formula: " + notVar);
            //System.out.println("Length: " + (length + 1) + " AnzTrue: " + anzTrue);
            addFormula(var);
            System.out.println("Added Formula: " + var);
            //System.out.println("Length: " + (length + 1) + " AnzTrue: " + (anzTrue + 1));
        }
    }


    private void removeMutualExclusiveEvents(List<Event> eventList, int numberOfWantedEvents){
        ExecutionAnalysis exec = task.getAnalysisContext().get(ExecutionAnalysis.class);
        for(int i = 1; i < eventList.size(); i++){
            for(int j = 0; j < i; j++){
                if(exec.areMutuallyExclusive(eventList.get(i), eventList.get(j))){
                    System.out.println("Tuple " + eventList.get(j) + " is mutually exclusive to Tuple " + eventList.get(i));
                    eventList.remove(i);
                    i--;
                }

            }
            if(i == numberOfWantedEvents){
                return;
            }
        }
        System.out.println("I searched for " + numberOfWantedEvents + " Tuples, but only found " + eventList.size() + ".");

    }


    private void removeMutualExclusiveTuples(List<Tuple> tupleList, int numberOfWantedTuples){
        for(int i = 1; i < tupleList.size(); i++){
            for(int j = 0; j < i; j++){
                if(areMutuallyExclusiv(tupleList.get(i), tupleList.get(j))){
                    System.out.println("Tuple " + tupleList.get(j) + " is mutually exclusive to Tuple " + tupleList.get(i));
                    tupleList.remove(i);
                    i--;
                }

            }
            if(i == numberOfWantedTuples){
                return;
            }
        }
        System.out.println("I searched for " + numberOfWantedTuples + " Tuples, but only found " + tupleList.size() + ".");

    }

    private boolean areMutuallyExclusiv(Tuple firstTuple, Tuple secondTuple){
        ExecutionAnalysis exec = task.getAnalysisContext().get(ExecutionAnalysis.class);
        return exec.areMutuallyExclusive(firstTuple.getFirst(), secondTuple.getFirst())
                || exec.areMutuallyExclusive(firstTuple.getFirst(), secondTuple.getSecond())
                || exec.areMutuallyExclusive(firstTuple.getSecond(), secondTuple.getFirst())
                || exec.areMutuallyExclusive(firstTuple.getSecond(), secondTuple.getSecond());
    }
}
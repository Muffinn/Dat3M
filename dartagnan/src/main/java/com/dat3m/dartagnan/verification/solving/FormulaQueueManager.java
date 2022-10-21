package com.dat3m.dartagnan.verification.solving;

import com.dat3m.dartagnan.wmm.utils.Tuple;

import java.util.BitSet;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FormulaQueueManager {
    //private Queue<BooleanFormula> formulaQueue;
    private Queue<BitSet> bitsetQueue;
    private List<Tuple> tupleList;
    //private SolverContext ctx;
    //private BooleanFormulaManager bmgr;
    //private VerificationTask task;
    //private Context analysisContext;

    private QueueType queueType;
    private int queueTypeSettingInt1;
    private int queueTypeSettingInt2;
    private String relationName;

    public FormulaQueueManager(){
        this.bitsetQueue = new ConcurrentLinkedQueue<BitSet>();
    }

    public FormulaQueueManager(QueueType queueTypeSetting, int queueTypeSettingInt1, int queueTypeSettingInt2){
        this.bitsetQueue = new ConcurrentLinkedQueue<BitSet>();
        populateFormulaQueue(queueTypeSetting, queueTypeSettingInt1, queueTypeSettingInt2);
        this.queueType = queueTypeSetting;
        this.queueTypeSettingInt1 = queueTypeSettingInt1;
        this.queueTypeSettingInt2 = queueTypeSettingInt2;
    }

    public synchronized BitSet getNextBitSet() {
        return bitsetQueue.remove();
    }


    private void addBitSet(BitSet addedBitSet){
        bitsetQueue.add(addedBitSet);
    }

    public int getQueueSize(){return bitsetQueue.size();}

    public synchronized int getQueueTypeSettingInt1(){return queueTypeSettingInt1;}

    public synchronized int getQueueTypeSettingInt2(){return queueTypeSettingInt2;}

    public synchronized QueueType getQueueType() {return queueType;}

    public void setRelationName(String relationName){this.relationName = relationName;}

    public void setQueueSettings(QueueType queueType, int queueTypeSettingInt1, int queueTypeSettingInt2){
        this.queueType = queueType;
        this.queueTypeSettingInt2 = queueTypeSettingInt2;
        this.queueTypeSettingInt1 = queueTypeSettingInt1;
    }

    public synchronized String getRelationName() {return relationName;}

    public void setTupleList(List<Tuple> tupleList){
        this.tupleList = tupleList;
    }

    public synchronized List<Tuple> getTupleList(){return tupleList;}

    public void populateFormulaQueue(QueueType queueTypeSetting, int queueTypeSettingInt1, int queueTypeSettingInt2){
        this.setQueueSettings(queueTypeSetting, queueTypeSettingInt1, queueTypeSettingInt2);
        switch(queueTypeSetting){
            case EMPTY:
            case SINGLE_LITERAL:
                System.out.println("EMPTY AND SINGLE_LITERAL are not implemented"); //TODO implement :)
            case RELATIONS_SORT:
            case RELATIONS_SHUFFLE:
            case MUTUALLY_EXCLUSIVE_SHUFFLE:
            case MUTUALLY_EXCLUSIVE_SORT:
            case EVENTS:
            case MUTUALLY_EXCLUSIVE_EVENTS:
                createTrueFalseBitQueue(queueTypeSettingInt1, queueTypeSettingInt2);
                break;
        }

        return;
    }

    private void createTrueFalseBitQueue(int maxLength, int maxTrue){
        BitSet bitSet = new BitSet(maxLength);
        recursiveTrueFalseBitQueue(bitSet, 0, maxLength, 0, maxTrue);
    }

    private void recursiveTrueFalseBitQueue(BitSet currentBitSet, int currentLength, int maxLength, int currentTrue, int maxTrue){
        currentBitSet.set(currentLength);
        if(!(currentTrue + 1 == maxTrue || currentLength + 1 == maxLength)){
            recursiveTrueFalseBitQueue(currentBitSet, currentLength + 1, maxLength, currentTrue + 1, maxTrue);
        } else {
            addBitSet((BitSet)currentBitSet.clone());
            System.out.println("Added BitSet: " + currentBitSet);
        }

        currentBitSet.clear(currentLength);

        if(!(currentTrue + 1 == maxTrue || currentLength + 1 == maxLength)){
            recursiveTrueFalseBitQueue(currentBitSet, currentLength + 1, maxLength,currentTrue, maxTrue);
        } else {
            addBitSet((BitSet)currentBitSet.clone());
            System.out.println("Added BitSet: " + currentBitSet);
        }
    }


    /*public void relationTuplesMutuallyExlusive(int maxLength, int maxTrue, List<Tuple> tupleList, String relationName){
        removeMutualExclusiveTuples(tupleList, maxLength);
        int newMaxLength = Math.min(tupleList.size(), maxLength);
        int newMaxTrue = Math.min(maxTrue, newMaxLength);
        relationTuples(newMaxLength, newMaxTrue, tupleList, relationName);
    }*/

    /*public void relationTuples(int maxLength, int maxTrue, List<Tuple> tupleList, String relationName)
        throws IllegalArgumentException{
        if(maxLength > tupleList.size()){
            throw new IllegalArgumentException("Tuplelist of size " + tupleList.size() + " contains to few items to fill Formula of size " + maxLength);
        }
        BooleanFormula newFormula = bmgr.makeTrue();

        recursiveRelationTuples(newFormula,0, maxLength, 0, maxTrue, tupleList, relationName);

    }*/

    /*private void recursiveRelationTuples(BooleanFormula currentFormula, int length, int maxLength, int anzTrue, int maxTrue, List<Tuple> tupleList, String relationName){

        BooleanFormula var = task.getMemoryModel().getRelation(relationName).getSMTVar(tupleList.get(length), encodingctx);
        BooleanFormula notVar = bmgr.not(var);
        var = bmgr.and(var, currentFormula);
        notVar = bmgr.and(notVar, currentFormula);


        if(!(anzTrue + 1 == maxTrue || length + 1 == maxLength)){
            recursiveRelationTuples(notVar, length + 1, maxLength, anzTrue, maxTrue, tupleList, relationName);
            recursiveRelationTuples(var, length + 1, maxLength,anzTrue + 1, maxTrue, tupleList, relationName);
        } else {
            oldAddFormula(notVar);
            //System.out.println("Added Formula: " + notVar);
            //System.out.println("Length: " + (length + 1) + " AnzTrue: " + anzTrue);
            oldAddFormula(var);
            //.out.println("Added Formula: " + var);
            //System.out.println("Length: " + (length + 1) + " AnzTrue: " + (anzTrue + 1));
        }
    }*/

    /*public void createTrues(int numberOfTrues){
        for (int i = 0; i < numberOfTrues; i++){
            oldAddFormula(bmgr.makeTrue());
        }
    }*/

    /*public void mutuallyExclusiveEventsQueue(int maxLength, int maxTrue, List<Event> eventList){
        removeMutualExclusiveEvents(eventList, maxLength);
        int newMaxLength = Math.min(eventList.size(), maxLength);
        int newMaxTrue = Math.min(maxTrue, newMaxLength);
        eventsQueue(newMaxLength, newMaxTrue, eventList);
    }*/

    /*public void eventsQueue(int maxLength, int maxTrue, List<Event> eventList)
        throws IllegalArgumentException{
            if(maxLength > eventList.size()){
                throw new IllegalArgumentException("Eventlist of size " + eventList.size() + " contains to few items to fill Formula of size " + maxLength);
            }
            BooleanFormula newFormula = bmgr.makeTrue();

            recursiveEventsQueue(newFormula, 0, maxLength, 0, maxTrue, eventList);
    }*/

    /*private void recursiveEventsQueue(BooleanFormula currentFormula, int length, int maxLength, int anzTrue, int maxTrue, List<Event> eventList){

        BooleanFormula var = eventList.get(length).exec(); //encodingContext.exec(evebtList.get(length));
        BooleanFormula notVar = bmgr.not(var);
        var = bmgr.and(var, currentFormula);
        notVar = bmgr.and(notVar, currentFormula);


        if(!(anzTrue + 1 == maxTrue || length + 1 == maxLength)){
            recursiveEventsQueue(notVar, length + 1, maxLength, anzTrue, maxTrue, eventList);
            recursiveEventsQueue(var, length + 1, maxLength,anzTrue + 1, maxTrue, eventList);
        } else {
            oldAddFormula(notVar);
            System.out.println("Added Formula: " + notVar);
            //System.out.println("Length: " + (length + 1) + " AnzTrue: " + anzTrue);
            oldAddFormula(var);
            System.out.println("Added Formula: " + var);
            //System.out.println("Length: " + (length + 1) + " AnzTrue: " + (anzTrue + 1));
        }
    }*/


    /*private void removeMutualExclusiveEvents(List<Event> eventList, int numberOfWantedEvents){
        ExecutionAnalysis exec = analysisContext.get(ExecutionAnalysis.class);
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
                if(areMutuallyExclusive(tupleList.get(i), tupleList.get(j))){
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

    private boolean areMutuallyExclusive(Tuple firstTuple, Tuple secondTuple){
        ExecutionAnalysis exec = analysisContext.get(ExecutionAnalysis.class);
        return exec.areMutuallyExclusive(firstTuple.getFirst(), secondTuple.getFirst())
                || exec.areMutuallyExclusive(firstTuple.getFirst(), secondTuple.getSecond())
                || exec.areMutuallyExclusive(firstTuple.getSecond(), secondTuple.getFirst())
                || exec.areMutuallyExclusive(firstTuple.getSecond(), secondTuple.getSecond());
    }*/
}
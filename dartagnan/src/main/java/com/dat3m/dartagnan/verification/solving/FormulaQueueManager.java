package com.dat3m.dartagnan.verification.solving;

import ap.Prover;
import com.dat3m.dartagnan.encoding.EncodingContext;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.wmm.Relation;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverContext;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FormulaQueueManager {

    private static final Logger logger = LogManager.getLogger(AssumeSolver.class);


    //private Queue<BooleanFormula> formulaQueue;
    private Queue<BitSet[]> bitsetQueue;
    private List<Tuple> tupleList;
    private List<Event> eventList;
    //private SolverContext ctx;
    //private BooleanFormulaManager bmgr;
    //private VerificationTask task;
    //private Context analysisContext;

    private QueueType queueType;
    private int queueTypeSettingInt1;
    private int queueTypeSettingInt2;
    private String relationName;

    public FormulaQueueManager(){
        this.bitsetQueue = new ConcurrentLinkedQueue<BitSet[]>();
    }

    public FormulaQueueManager(QueueType queueTypeSetting, int queueTypeSettingInt1, int queueTypeSettingInt2)
                 throws InvalidConfigurationException{
        this.bitsetQueue = new ConcurrentLinkedQueue<BitSet[]>();
        populateFormulaQueue(queueTypeSetting, queueTypeSettingInt1, queueTypeSettingInt2);
        this.queueType = queueTypeSetting;
        this.queueTypeSettingInt1 = queueTypeSettingInt1;
        this.queueTypeSettingInt2 = queueTypeSettingInt2;
    }

    public synchronized BitSet[] getNextBitSet() {
        return bitsetQueue.remove();
    }


    private void addBitSet(BitSet[] addedBitSetArray){
        bitsetQueue.add(addedBitSetArray);
        logger.info("Created BitSet Pair " + addedBitSetArray[0] + " and not " + addedBitSetArray[1]);
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

    public void setEventList(List<Event> tupleList){
        this.eventList = tupleList;
    }

    public synchronized List<Tuple> getTupleList(){return tupleList;}

    public void populateFormulaQueue(QueueType queueTypeSetting, int queueTypeSettingInt1, int queueTypeSettingInt2)
    throws InvalidConfigurationException{
        this.setQueueSettings(queueTypeSetting, queueTypeSettingInt1, queueTypeSettingInt2);
        switch(queueTypeSetting){
            case EMPTY:
                createEmptyBitSetQueue(queueTypeSettingInt1);
                break;
            case SINGLE_LITERAL:
                logger.warn("EMPTY AND SINGLE_LITERAL are not implemented"); //TODO implement :)
            case RELATIONS_SORT:
            case RELATIONS_SHUFFLE:
            case MUTUALLY_EXCLUSIVE_SHUFFLE:
            case MUTUALLY_EXCLUSIVE_SORT:
            case EVENTS:
            case MUTUALLY_EXCLUSIVE_EVENTS:
                createTreeStyleBitSetQueue(queueTypeSettingInt1, queueTypeSettingInt2);
                break;
        }

    }

    private void createEmptyBitSetQueue(int nrOfBitSets) throws InvalidConfigurationException {
        if(nrOfBitSets < 1){
            throw new InvalidConfigurationException("EmptyBitSetQueue must contain at least one item.");
        }

        for (int i = 0; i < nrOfBitSets; i++){
            BitSet[] bitSetPair = new BitSet[2];
            bitSetPair[0] = new BitSet();
            bitSetPair[1] = new BitSet();
            addBitSet(bitSetPair);
        }
    }


    private void createTreeStyleBitSetQueue(int nrOfTrees, int treeDepth) throws InvalidConfigurationException{
        if(nrOfTrees < 1){
            throw new InvalidConfigurationException("TreeStyleBitSetQueue creation failed. There must be at least one Tree.");
        }
        if(treeDepth < 0){
            throw new InvalidConfigurationException("TreeStyleBitSetQueue creation failed. Tree Depth can't be negative.");
        }

        BitSet varBitSet = new BitSet(nrOfTrees + treeDepth);
        BitSet notVarBitSet = new BitSet(nrOfTrees + treeDepth);

        int i = 0;
        while (i + 1 < nrOfTrees){
            varBitSet.set(i);
            createTreeBitSet(treeDepth, varBitSet, notVarBitSet, i + 1);
            varBitSet.clear(i);
            notVarBitSet.set(i);
            i++;
        }
        createTreeBitSet(treeDepth, varBitSet, notVarBitSet, i);
    }

    private void createTreeBitSet(int treeDepth, BitSet varBitSet, BitSet notVarBitSet, int startPoint){

        if(treeDepth == 0){
            BitSet[] newBitSetPair = new BitSet[2];
            newBitSetPair[0]= (BitSet)varBitSet.clone();
            newBitSetPair[1] = (BitSet)notVarBitSet.clone();
            addBitSet(newBitSetPair);
            return;
        }

        varBitSet.set(startPoint);
        if(treeDepth == 1){
            BitSet[] newBitSetPair = new BitSet[2];
            newBitSetPair[0]= (BitSet)varBitSet.clone();
            newBitSetPair[1] = (BitSet)notVarBitSet.clone();
            addBitSet(newBitSetPair);
            varBitSet.clear(startPoint);
            notVarBitSet.set(startPoint);
            newBitSetPair = new BitSet[2];
            newBitSetPair[0]= (BitSet)varBitSet.clone();
            newBitSetPair[1] = (BitSet)notVarBitSet.clone();
            addBitSet(newBitSetPair);
            notVarBitSet.clear(startPoint);
        } else {
            createTreeBitSet(treeDepth - 1, varBitSet, notVarBitSet, startPoint + 1);
            varBitSet.clear(startPoint);
            notVarBitSet.set(startPoint);
            createTreeBitSet(treeDepth - 1, varBitSet, notVarBitSet, startPoint + 1);
            notVarBitSet.clear(startPoint);
        }
    }


    /*private void recursiveTrueFalseBitQueue(BitSet currentBitSet, int currentLength, int maxLength, int currentTrue, int maxTrue){
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
    }*/

    public BooleanFormula generateRelationFormula(SolverContext ctx, EncodingContext encodingCTX, VerificationTask mainTask, int threadID){
        BitSet[] myBitSets = getNextBitSet();
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
        BooleanFormula myFormula = bmgr.makeTrue();
        String relationName = getRelationName();
        int i = 0;
        while(myBitSets[0].get(i) || myBitSets[1].get(i)){
            if (myBitSets[0].get(i)){
                BooleanFormula var = mainTask.getMemoryModel().getRelation(relationName).getSMTVar(tupleList.get(i), encodingCTX);
                myFormula = bmgr.and(var, myFormula);
            } else if(myBitSets[1].get(i)){
                BooleanFormula notVar = bmgr.not(mainTask.getMemoryModel().getRelation(relationName).getSMTVar(tupleList.get(i), encodingCTX));
                myFormula = bmgr.and(notVar, myFormula);
            }
            i++;
            if(i >= myBitSets[0].size()){
                break;
            }
        }
        logger.info("Thread " + threadID + ": " +  "generated Formula: " + myFormula);
        return myFormula;
    }


    public List<List<Tuple>> generateRelationListPair(int threadID){
        List<List<Tuple>> relationListPair = new ArrayList<List<Tuple>>(2);
        relationListPair.add(new ArrayList<Tuple>());
        relationListPair.add(new ArrayList<Tuple>());


        BitSet[] myBitSets = getNextBitSet();
        int i = 0;
        while(myBitSets[0].get(i) || myBitSets[1].get(i)){
            if (myBitSets[0].get(i)){
                relationListPair.get(0).add(tupleList.get(i));

            } else if(myBitSets[1].get(i)){
                relationListPair.get(1).add(tupleList.get(i));
            }
            i++;
            if(i >= myBitSets[0].size()){
                break;
            }
        }
        return relationListPair;
    }

    public BooleanFormula generateEventFormula(SolverContext ctx, EncodingContext encodingCTX, int threadID){
        BitSet[] myBitSets = getNextBitSet();
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
        BooleanFormula myFormula = bmgr.makeTrue();



        int i = 0;
        while(myBitSets[0].get(i) || myBitSets[1].get(i)){
            if (myBitSets[0].get(i)){

                BooleanFormula var = encodingCTX.execution(eventList.get(i));
                myFormula = bmgr.and(var, myFormula);
            } else if(myBitSets[1].get(i)){
                BooleanFormula notVar = bmgr.not(encodingCTX.execution(eventList.get(i)));
                myFormula = bmgr.and(notVar, myFormula);
            }
            i++;
            if(i >= myBitSets[0].size()){
                break;
            }
        }
        logger.info("Thread " + threadID + ": " +  "generated Formula: " + myFormula);
        return myFormula;
    }

    public List<List<Event>> generateEventListPair(int threadID){
        List<List<Event>> eventListPair = new ArrayList<List<Event>>(2);
        eventListPair.add(new ArrayList<Event>());
        eventListPair.add(new ArrayList<Event>());


        BitSet[] myBitSets = getNextBitSet();
        int i = 0;
        while(myBitSets[0].get(i) || myBitSets[1].get(i)){
            if (myBitSets[0].get(i)){
                eventListPair.get(0).add(eventList.get(i));

            } else if(myBitSets[1].get(i)){
                eventListPair.get(1).add(eventList.get(i));
            }
            i++;
            if(i >= myBitSets[0].size()){
                break;
            }
        }
        return eventListPair;
    }


    public BooleanFormula generateEmptyFormula(SolverContext ctx, int threadID){
        BooleanFormula myFormula = ctx.getFormulaManager().getBooleanFormulaManager().makeTrue();
        logger.info("Thread " + threadID + ": " +  "generated Formula: " + myFormula);
        return myFormula;
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
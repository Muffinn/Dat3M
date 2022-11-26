package com.dat3m.dartagnan.verification.solving;

import com.dat3m.dartagnan.encoding.EncodingContext;
import com.dat3m.dartagnan.program.analysis.ExecutionAnalysis;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.verification.Context;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverContext;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SplittingManager {

    private static final Logger logger = LogManager.getLogger(AssumeSolver.class);


    //private Queue<BooleanFormula> formulaQueue;
    private final Queue<BitSet[]> bitsetQueue;
    private List<Tuple> tupleList;
    private List<Event> eventList;
    //private SolverContext ctx;
    //private BooleanFormulaManager bmgr;
    //private VerificationTask task;
    //private Context analysisContext;

    private String relationName;
    private final ParallelSolverConfiguration parallelConfig;

    public SplittingManager(ParallelSolverConfiguration parallelConfig)
            throws InvalidConfigurationException{
        this.bitsetQueue = new ConcurrentLinkedQueue<BitSet[]>();
        this.parallelConfig = parallelConfig;
        populateFormulaQueue();
    }



    public synchronized BitSet[] getNextBitSet() {
        return bitsetQueue.remove();
    }


    private void addBitSet(BitSet[] addedBitSetArray){
        bitsetQueue.add(addedBitSetArray);
        logger.info("Created BitSet Pair " + addedBitSetArray[0] + " and not " + addedBitSetArray[1]);
    }

    public int getQueueSize(){return bitsetQueue.size();}


    public void setRelationName(String relationName){this.relationName = relationName;}


    public synchronized String getRelationName() {return relationName;}

    public void setTupleList(List<Tuple> tupleList){
        this.tupleList = tupleList;
    }

    public void setEventList(List<Event> tupleList){
        this.eventList = tupleList;
    }

    public List<Tuple> getTupleList(){return tupleList;}

    public List<Event> getEventList() {return eventList;}

    public void populateFormulaQueue()
            throws InvalidConfigurationException{
        switch(parallelConfig.getSplittingStyle()){
            case NO_SPLITTING_STYLE:
                createEmptyBitSetQueue(parallelConfig.getQueueSettingIntN());
                break;
            case LINEAR_AND_BINARY_SPLITTING_STYLE:
                createLinearAndBinarySplitting(parallelConfig.getQueueSettingIntN(), parallelConfig.getQueueSettingIntM());
                break;
            case LINEAR_SPLITTING_STYLE:
                createLinearAndBinarySplitting(parallelConfig.getQueueSettingIntN(), 0);
                break;
            case BINARY_SPLITTING_STYLE:
                createLinearAndBinarySplitting(1, parallelConfig.getQueueSettingIntN());
                break;
            default:
                throw(new InvalidConfigurationException(parallelConfig.getSplittingStyle().name() + "is not supported by populateFormulaQueue."));
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


    private void createLinearAndBinarySplitting(int nrOfTrees, int treeDepth) throws InvalidConfigurationException{
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
            createBinarySplitting(treeDepth, varBitSet, notVarBitSet, i + 1);
            varBitSet.clear(i);
            notVarBitSet.set(i);
            i++;
        }
        createBinarySplitting(treeDepth, varBitSet, notVarBitSet, i);
    }

    private void createBinarySplitting(int treeDepth, BitSet varBitSet, BitSet notVarBitSet, int startPoint){

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
            createBinarySplitting(treeDepth - 1, varBitSet, notVarBitSet, startPoint + 1);
            varBitSet.clear(startPoint);
            notVarBitSet.set(startPoint);
            createBinarySplitting(treeDepth - 1, varBitSet, notVarBitSet, startPoint + 1);
            notVarBitSet.clear(startPoint);
        }
    }

    public void orderEvents() {
        switch (parallelConfig.getSplittingObjectSelection()){
            case NO_SELECTION:
                break;

            case INDEX_SELECTION:
                sortEventsByID();
                break;

            case CHOSEN_SELECTION:
                sortEventsByID();
                Collections.shuffle(eventList, parallelConfig.getShuffleRandom());
                logger.info("Random Shuffle Seed: " + parallelConfig.getRandomSeed() + " .");
                sortToFront(parallelConfig.getChosenEvents());
                break;

            case RANDOM_SELECTION:
            case SEEDED_RANDOM_SELECTION:
                sortEventsByID();
                Collections.shuffle(eventList, parallelConfig.getShuffleRandom());
                logger.info("Random Shuffle Seed: " + parallelConfig.getRandomSeed() + " .");
                break;

            default:
                throw(new Error("Formula Order " + parallelConfig.getSplittingObjectSelection().name() + " is not supported in ParallelRefinement."));
        }
    }
    public void orderTuples() {
        switch (parallelConfig.getSplittingObjectSelection()){
            case NO_SELECTION:
                break;

            case INDEX_SELECTION:
                sortTuplesByID();
                break;
            case RANDOM_SELECTION:
            case SEEDED_RANDOM_SELECTION:
                sortTuplesByID();
                Collections.shuffle(tupleList, parallelConfig.getShuffleRandom());
                logger.info("Random Shuffle Seed: " + parallelConfig.getRandomSeed() + " .");
                break;

            default:
                throw(new Error("Formula Order " + parallelConfig.getSplittingObjectSelection().name() + " is not supported in ParallelRefinement."));
        }

    }

    public void filterEvents(Context analysisContext) {
        switch (parallelConfig.getSplittingObjectFilter()){
            case NO_SO_FILTER:
                break;

            case IMPLIES_SO_FILTER:
                filterImpliedEvents(analysisContext);
                break;

            case MUTUALLY_EXCLUSIVE_SO_FILTER:
                filterMEEvents(analysisContext);
                break;

            case IMP_AND_ME_SO_FILTER:
                filterImpliedAndMEEvents(analysisContext);
                break;

            default:
                throw(new Error("Formula Order " + parallelConfig.getSplittingObjectFilter().name() + " is not supported in ParallelRefinement."));
        }
    }

    public void filterTuples(Context analysisContext) {
        switch (parallelConfig.getSplittingObjectFilter()){
            case NO_SO_FILTER:
                break;

            case IMPLIES_SO_FILTER:
                filterImpliedTuples();
                break;

            case MUTUALLY_EXCLUSIVE_SO_FILTER:
                filterMETuples(analysisContext);
                break;

            case IMP_AND_ME_SO_FILTER:
                filterImpliedAndMETuples();
                break;

            default:
                throw(new Error("Formula Order " + parallelConfig.getSplittingObjectFilter().name() + " is not supported in ParallelRefinement."));
        }
    }



    public BooleanFormula generateRelationFormula(SolverContext ctx, EncodingContext encodingCTX, VerificationTask mainTask, int threadID, ThreadStatisticManager myStatManager){
        BitSet[] myBitSets = getNextBitSet();
        myStatManager.setBitSetPair(myBitSets);
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


    public List<List<Tuple>> generateRelationListPair(int threadID, ThreadStatisticManager myStatisticManager){
        List<List<Tuple>> relationListPair = new ArrayList<List<Tuple>>(2);
        relationListPair.add(new ArrayList<Tuple>());
        relationListPair.add(new ArrayList<Tuple>());


        BitSet[] myBitSets = getNextBitSet();
        myStatisticManager.setBitSetPair(myBitSets);
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

    public BooleanFormula generateEventFormula(SolverContext ctx, EncodingContext encodingCTX, int threadID, ThreadStatisticManager myStatisticManager){
        BitSet[] myBitSets = getNextBitSet();
        myStatisticManager.setBitSetPair(myBitSets);
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

    public List<List<Event>> generateEventListPair(int threadID, ThreadStatisticManager myStatisticManager){
        List<List<Event>> eventListPair = new ArrayList<List<Event>>(2);
        eventListPair.add(new ArrayList<Event>());
        eventListPair.add(new ArrayList<Event>());


        BitSet[] myBitSets = getNextBitSet();
        myStatisticManager.setBitSetPair(myBitSets);
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


    private void sortEventsByID(){
        eventList.sort(new Comparator<Event>() {
            @Override
            public int compare(Event e1, Event e2) {
                return Integer.compare(e1.getCId(), e2.getCId());
            }
        });
    }

    private void sortToFront(int[] chosenIDs){
        for (int i=0; i< chosenIDs.length; i++){
            for(int j = 0; j < eventList.size(); j++){
                if(eventList.get(j).getCId() == chosenIDs[i]){
                    Event e = eventList.get(i);
                    eventList.set(i, eventList.get(j));
                    eventList.set(j, e);
                    break;
                }
            }

        }
    }

    private void sortTuplesByID(){
        tupleList.sort(new Comparator<Tuple>() {
            @Override
            public int compare(Tuple t1, Tuple t2) {
                return Integer.compare(t1.getFirst().getCId(), t2.getFirst().getCId());
            }
        });
    }

    private void filterMEEvents(Context analysisContext){
        int formulaLength = parallelConfig.getFormulaLength();
        int foundItems = 1;
        ArrayList<Event> filteredEvents = new ArrayList<Event>();
        int i = 1;
        while (foundItems < formulaLength && i < eventList.size()){
            boolean isME = false;
            for (int j = 0; (j < i) && (!isME); j++){
                isME = areEventsMutuallyExclusive(eventList.get(i), eventList.get(j), analysisContext);
            }
            if(isME){
               filteredEvents.add(this.eventList.get(i));
            }else{
                foundItems++;
            }

            i++;
        }

        if(foundItems < formulaLength){
            logger.warn("Warning: Chosen Filter filtered to many Events. Not alle filtered Event will be removed.");
            while (foundItems < formulaLength){
                filteredEvents.remove(filteredEvents.get(0));
                foundItems++;
            }
        }
        eventList.removeAll(filteredEvents);
        logger.info("Filtered Events: " + filteredEvents);
    }



    private void filterImpliedEvents(Context analysisContext){
        int formulaLength = parallelConfig.getFormulaLength();
        int foundItems = 1;
        ArrayList<Event> filteredEvents = new ArrayList<Event>();
        int i = 1;
        while (foundItems < formulaLength && i < eventList.size()){
            boolean isImplied = false;
            for (int j = 0; (j < i) && (!isImplied); j++){
                isImplied = isEventImplied(eventList.get(j), eventList.get(i), analysisContext);
            }
            if(isImplied){
                filteredEvents.add(this.eventList.get(i));
            }else{
                foundItems++;
            }

            i++;
        }

        if(foundItems < formulaLength){
            logger.warn("Warning: Chosen Filter filtered to many Events. Not alle filtered Event will be removed.");
            while (foundItems < formulaLength){
                filteredEvents.remove(filteredEvents.get(0));
                foundItems++;
            }
        }
        eventList.removeAll(filteredEvents);
        logger.info("Filtered Events: " + filteredEvents);
    }

    private void filterImpliedAndMEEvents(Context analysisContext){
        int formulaLength = parallelConfig.getFormulaLength();
        int foundItems = 1;
        ArrayList<Event> filteredEvents = new ArrayList<Event>();
        int i = 1;
        while (foundItems < formulaLength && i < eventList.size()){
            boolean isMEorImplied = false;
            for (int j = 0; (j < i) && (!isMEorImplied); j++){
                isMEorImplied = isEventImplied(eventList.get(j), eventList.get(i), analysisContext) || areEventsMutuallyExclusive(eventList.get(i), eventList.get(j), analysisContext);
            }
            if(isMEorImplied){
                filteredEvents.add(this.eventList.get(i));
            }else{
                foundItems++;
            }

            i++;
        }

        if(foundItems < formulaLength){
            logger.warn("Warning: Chosen Filter filtered to many Events. Not alle filtered Event will be removed.");
            while (foundItems < formulaLength){
                filteredEvents.remove(filteredEvents.get(0));
                foundItems++;
            }
        }
        eventList.removeAll(filteredEvents);
        logger.info("Filtered Events: " + filteredEvents);

    }

    private boolean areEventsMutuallyExclusive(Event e1, Event e2, Context analysisContext){
            ExecutionAnalysis exec = analysisContext.get(ExecutionAnalysis.class);

            return exec.areMutuallyExclusive(e1, e2);
    }

    private boolean isEventImplied(Event start, Event implied, Context analysisContext){
        ExecutionAnalysis exec = analysisContext.get(ExecutionAnalysis.class);

        return exec.isImplied(start, implied);
    }

    private void filterMETuples(Context analysisContext){
        int formulaLength = parallelConfig.getFormulaLength();
        int foundItems = 1;
        ArrayList<Tuple> filteredTuples = new ArrayList<Tuple>();
        int i = 1;
        while (foundItems < formulaLength && i < tupleList.size()){
            boolean isME = false;
            for (int j = 0; (j < i) && (!isME); j++){
                isME = areTuplesMutuallyExclusive(tupleList.get(i), tupleList.get(j), analysisContext);
            }
            if(isME){
                filteredTuples.add(this.tupleList.get(i));
            }else{
                foundItems++;
            }

            i++;
        }

        if(foundItems < formulaLength){
            logger.warn("Warning: Chosen Filter filtered to many Tuples. Not alle filtered Event will be removed.");
            while (foundItems < formulaLength){
                filteredTuples.remove(filteredTuples.get(0));
                foundItems++;
            }
        }
        tupleList.removeAll(filteredTuples);
        logger.info("Filtered Tuples: " + filteredTuples);
    }

    private void filterImpliedTuples(){
        int formulaLength = parallelConfig.getFormulaLength();
        logger.info("Filter does nothing");
    }

    private void filterImpliedAndMETuples(){
        int formulaLength = parallelConfig.getFormulaLength();
        logger.info("Filter does nothing");
    }


    private boolean areTuplesMutuallyExclusive(Tuple t1, Tuple t2, Context analysisContext){
        return areEventsMutuallyExclusive(t1.getFirst(), t2.getFirst(), analysisContext)
                || areEventsMutuallyExclusive(t1.getFirst(), t2.getSecond(), analysisContext)
                || areEventsMutuallyExclusive(t1.getSecond(), t2.getFirst(), analysisContext)
                || areEventsMutuallyExclusive(t1.getSecond(), t2.getSecond(), analysisContext);
    }



    private boolean isTupleImplied(Tuple impliedTuple, Tuple implyingTuple, Context analysisContext){
        return false;
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
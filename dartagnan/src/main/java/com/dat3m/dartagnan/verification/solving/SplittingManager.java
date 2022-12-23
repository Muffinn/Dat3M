package com.dat3m.dartagnan.verification.solving;

import com.dat3m.dartagnan.encoding.EncodingContext;
import com.dat3m.dartagnan.program.Thread;
import com.dat3m.dartagnan.program.analysis.ExecutionAnalysis;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.verification.Context;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.wmm.Relation;
import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;
import com.dat3m.dartagnan.wmm.relation.RelationNameRepository;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import java_cup.Main;
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

    private int filteredLiterals = 0;

    public SplittingManager(ParallelSolverConfiguration parallelConfig)
            throws InvalidConfigurationException{
        this.bitsetQueue = new ConcurrentLinkedQueue<BitSet[]>();
        this.parallelConfig = parallelConfig;
        populateBitSetQueue();
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

    public void populateBitSetQueue()
            throws InvalidConfigurationException{
        switch(parallelConfig.getSplittingStyle()){
            case NO_SPLITTING_STYLE:
                createEmptyBitSetQueue(parallelConfig.getSplittingIntN());
                break;
            case LINEAR_AND_BINARY_SPLITTING_STYLE:
                createLinearAndBinarySplitting(parallelConfig.getSplittingIntN(), parallelConfig.getSplittingIntM());
                break;
            case LINEAR_SPLITTING_STYLE:
                createLinearAndBinarySplitting(parallelConfig.getSplittingIntN(), 0);
                break;
            case BINARY_SPLITTING_STYLE:
                createLinearAndBinarySplitting(1, parallelConfig.getSplittingIntN());
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

    public void orderEvents(Context analysisContext, VerificationTask myTask) {
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
            case SCORE_SELECTION:
                sortEventsByScore(analysisContext, myTask);
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


    //...........................Event..Order.......................
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

    private void sortEventsByScore(Context analysisContext, VerificationTask myTask){

        List<Thread> threads =  eventList.get(0).getThread().getProgram().getThreads();
        float[] scores = new float[parallelConfig.getFormulaLength()];
        int[] cids = new int [parallelConfig.getFormulaLength()];
        Arrays.fill(cids, -1);
        for (Thread thread:threads){
            float tHighScore = 0;
            int cid = -1;
            for(Event e : thread.getEvents()){
                float eScore = derivedEventScore(e, analysisContext, myTask);
                if( eScore > tHighScore){
                    tHighScore = eScore;
                    cid = e.getCId();
                }
            }
            for(int i = 0; i < scores.length; i++){
                if(tHighScore > scores[i]){
                    float tempScore  = scores[i];
                    int tempCID = cids[i];
                    scores[i]  = tHighScore;
                    cids[i] = cid;
                    tHighScore = tempScore;
                    cid = tempCID;
                }
            }
        }
        sortToFront(cids);

    }

    private static int badEventScore(Event eventToScore, Context analysisContext){
        int meCount = 0;
        int impCount = 0;
        Thread thread = eventToScore.getThread();
        for (Event otherEvent: thread.getEvents()){
            if(areEventsMutuallyExclusive(otherEvent, eventToScore, analysisContext)){
                meCount++;
            }
            if(isEventImplied(eventToScore, otherEvent, analysisContext)){
                impCount++;
            }
        }
        int score = Math.min(meCount, impCount);
        return score;
    }

    private static int derivedEventScore(Event targetEvent, Context analysisContext, VerificationTask myTask){
        int trueScore = 0;
        int falseScore = 0;
        Thread thread = targetEvent.getThread();
        for (Event otherEvent: thread.getEvents()){
            if(areEventsMutuallyExclusive(otherEvent, targetEvent, analysisContext)){
                trueScore += simpleEventScore(otherEvent, analysisContext, myTask);
            }
            if(isEventImplied(otherEvent, targetEvent, analysisContext)){
                falseScore += simpleEventScore(otherEvent, analysisContext, myTask);
            }
        }

        //System.out.println("Event T " + targetEvent.getCId() + ": " + trueScore);

        //System.out.println("Event F " + targetEvent.getCId() + ": " + falseScore);

        int score = Math.min(trueScore, falseScore);
        //System.out.println("Event " + targetEvent.getCId() + ": " + score);
        return score;
    }

    private static int simpleEventScore(Event targetEvent, Context analysisContext, VerificationTask myTask){
        int score = 0;
        String relationRFName = RelationNameRepository.RF;
        Relation relationRF = myTask.getMemoryModel().getRelation(relationRFName);
        RelationAnalysis relationAnalysis = analysisContext.get(RelationAnalysis.class);
        RelationAnalysis.Knowledge knowledge = relationAnalysis.getKnowledge(relationRF);
        TupleSet rfEncodeSet = knowledge.getMaySet();
        List<Tuple> tupleListRF = new ArrayList<>(rfEncodeSet);
        for(Tuple t:tupleListRF){
            if (t.getFirst()==targetEvent || t.getSecond()==targetEvent){
                score++;
            }
        }

        String relationCOName = RelationNameRepository.CO;
        Relation relationCO = myTask.getMemoryModel().getRelation(relationCOName);
        RelationAnalysis.Knowledge knowledgeCO = relationAnalysis.getKnowledge(relationCO);
        TupleSet coEncodeSet = knowledgeCO.getMaySet();
        List<Tuple> tupleListCO = new ArrayList<>(coEncodeSet);
        for(Tuple t:tupleListCO){
            if (t.getFirst()==targetEvent || t.getSecond()==targetEvent){
                score++;
            }
        }

        /*String relationFRName = RelationNameRepository.FR;
        Relation relationFR = myTask.getMemoryModel().getRelation(relationFRName);
        RelationAnalysis.Knowledge knowledgeFR = relationAnalysis.getKnowledge(relationFR);
        TupleSet frEncodeSet = knowledgeFR.getMaySet();
        List<Tuple> tupleListFR = new ArrayList<>(frEncodeSet);
        for(Tuple t:tupleListFR){
            if (t.getFirst()==targetEvent || t.getSecond()==targetEvent){
                score++;
            }
        }*/
        //System.out.println("Event " + targetEvent.getCId() + ": " + score);

        return score;
    }

    private static int advancedEventScore(Event targetEvent, Context analysisContext, VerificationTask myTask){
        int trueScore = 0;
        int falseScore = 0;
        Thread thread = targetEvent.getThread();
        for (Event otherEvent: thread.getEvents()){
            if(areEventsMutuallyExclusive(otherEvent, targetEvent, analysisContext)){
                trueScore += simpleEventScore(otherEvent, analysisContext, myTask);
            }
            if(isEventImplied(otherEvent, targetEvent, analysisContext)){
                falseScore += simpleEventScore(otherEvent, analysisContext, myTask);
            }
        }

        //System.out.println("Event T " + targetEvent.getCId() + ": " + trueScore);

        //System.out.println("Event F " + targetEvent.getCId() + ": " + falseScore);

        int score = Math.min(trueScore, falseScore);
        //System.out.println("Event " + targetEvent.getCId() + ": " + score);
        return score;
    }


    //......................Filter..for..Splitting..Events.............
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
        filteredLiterals += filteredEvents.size();
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
                isImplied = isEventImplied(eventList.get(j), eventList.get(i), analysisContext)|| isEventImplied(eventList.get(i), eventList.get(j), analysisContext);
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
        filteredLiterals += filteredEvents.size();
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
                isMEorImplied = isEventImplied(eventList.get(j), eventList.get(i), analysisContext)|| isEventImplied(eventList.get(i), eventList.get(j), analysisContext) || areEventsMutuallyExclusive(eventList.get(i), eventList.get(j), analysisContext);
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
        filteredLiterals += filteredEvents.size();
        eventList.removeAll(filteredEvents);
        logger.info("Filtered Events: " + filteredEvents);

    }

    private static boolean areEventsMutuallyExclusive(Event e1, Event e2, Context analysisContext){
            ExecutionAnalysis exec = analysisContext.get(ExecutionAnalysis.class);

            return exec.areMutuallyExclusive(e1, e2);
    }

    private static boolean isEventImplied(Event start, Event implied, Context analysisContext){
        ExecutionAnalysis exec = analysisContext.get(ExecutionAnalysis.class);

        return exec.isImplied(start, implied);
    }

    //......................Filter..for..Splitting..Tuples..........
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

    public int getFilteredLiterals() {
        return filteredLiterals;
    }
}
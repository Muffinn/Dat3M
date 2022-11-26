package com.dat3m.dartagnan.verification.solving;

import com.dat3m.dartagnan.solver.caat4wmm.coreReasoning.CoreLiteral;
import com.dat3m.dartagnan.utils.logic.Conjunction;
import com.dat3m.dartagnan.utils.logic.DNF;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ParallelRefinementCollector {
    private int maxClauseLength;
    private final ArrayList<Conjunction<CoreLiteral>> fullReasonsList;
    private final ArrayList<ConcurrentLinkedQueue<Conjunction<CoreLiteral>>> reasonQueueList;
    private final ParallelSolverConfiguration parallelConfig;
    private int filterCount = 0;

    public void registerReasonQueue(ConcurrentLinkedQueue<Conjunction<CoreLiteral>> reasonQueue){
        synchronized (reasonQueueList){
            reasonQueueList.add(reasonQueue);
            synchronized(fullReasonsList){
                for (Conjunction<CoreLiteral> reason: fullReasonsList){
                    reasonQueue.add(reason);
                }
            }
        }
    }

    public void deregisterReasonQueue(Queue<Conjunction<CoreLiteral>> reasonQueue){
        synchronized(reasonQueueList){
            reasonQueueList.remove(reasonQueue);
        }
    }



    /*public synchronized List<Conjunction<CoreLiteral>> getReasonList() {
        List<Conjunction<CoreLiteral>> newList = new ArrayList<>(fullReasonsList);
        return newList;
    }*/

    private void addReasonToFullList(Conjunction<CoreLiteral> reason){
        synchronized (fullReasonsList){
            fullReasonsList.add(reason);
        }
    }

    /*private void removeReasonFromFullList(Conjunction<CoreLiteral> reason){
        //synchronized (fullReasonsList){
            fullReasonsList.remove(reason);
        //}
    }*/



    public ParallelRefinementCollector(int maxClauseLength, ParallelSolverConfiguration parallelConfig){
        this.maxClauseLength = maxClauseLength;
        fullReasonsList = new ArrayList<Conjunction<CoreLiteral>>();
        reasonQueueList = new ArrayList<ConcurrentLinkedQueue<Conjunction<CoreLiteral>>>();
        this.parallelConfig = parallelConfig;
    }



    public /*synchronized*/ void addReason(DNF<CoreLiteral> reason, ConcurrentLinkedQueue<Conjunction<CoreLiteral>> myQueue){
        List<Queue<Conjunction<CoreLiteral>>> reasonQueueListLocal;
        synchronized (reasonQueueList){
            reasonQueueListLocal = List.copyOf(reasonQueueList);
        }
        for(Conjunction<CoreLiteral> cube : reason.getCubes()){
            if(!isFiltered(cube, myQueue)){
                addReasonToFullList(cube);
                for(Queue<Conjunction<CoreLiteral>> queue : reasonQueueListLocal){
                    if (queue != myQueue){
                        queue.add(cube);
                    }
                }

            }
        }
;
    }

    private Boolean isFiltered(Conjunction<CoreLiteral> cube, ConcurrentLinkedQueue<Conjunction<CoreLiteral>> myQueue){
        switch (parallelConfig.getClauseSharingFilter()){
            case NO_CS_FILTER:
                return false;
            case NO_CLAUSE_SHARING:
                return true;
            case DUPLICATE_CS_FILTER:
                return duplicateClauseFilter(cube, myQueue);
            case IMPLIED_CS_FILTER:
                return impliedFilter(cube, myQueue);
            default:
                throw(new Error("ClauseSharingFilter not implemented yet: " + parallelConfig.getClauseSharingFilter().name()));
        }

    }

    private Boolean duplicateClauseFilter(Conjunction<CoreLiteral> cube, ConcurrentLinkedQueue<Conjunction<CoreLiteral>> myQueue){
        //synchronized(myQueue){
            for(Conjunction<CoreLiteral> foundCube: myQueue){
                if (cube.equals(foundCube) ) {
                    //|| cube.compareToPartial(foundCube) == OrderResult.EQ
                    filterCount++;
                    return true;
                }
                /*if (foundCube.isProperSubclauseOf(cube)) {
                    removeReasonFromFullList(foundCube);
                    filterCount++;
                }*/
            }
        //}
        return false;
    }

    private Boolean impliedFilter(Conjunction<CoreLiteral> cube, ConcurrentLinkedQueue<Conjunction<CoreLiteral>> myQueue){
        return false;
    }


}

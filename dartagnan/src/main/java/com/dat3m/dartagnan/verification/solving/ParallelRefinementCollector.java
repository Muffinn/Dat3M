package com.dat3m.dartagnan.verification.solving;

import com.dat3m.dartagnan.solver.caat4wmm.coreReasoning.CoreLiteral;
import com.dat3m.dartagnan.utils.logic.DNF;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class ParallelRefinementCollector {
    private int maxClauseLength;
    private final ArrayList<DNF<CoreLiteral>> fullReasonsList;
    private final ArrayList<Queue<DNF<CoreLiteral>>> reasonQueueList;

    public void registerReasonQueue(Queue<DNF<CoreLiteral>> reasonQueue){
        synchronized (reasonQueueList){
            reasonQueueList.add(reasonQueue);
            for (DNF<CoreLiteral> reason: fullReasonsList){
                reasonQueue.add(reason);
            }
        }
    }

    public void deregisterReasonQueue(Queue<DNF<CoreLiteral>> reasonQueue){
        synchronized(reasonQueueList){
            reasonQueueList.remove(reasonQueue);
        }
    }



    public synchronized List<DNF<CoreLiteral>> getReasonList() {
        List<DNF<CoreLiteral>> newList = new ArrayList<>(fullReasonsList);
        return newList;
    }





    public ParallelRefinementCollector(int maxClauseLength){
        this.maxClauseLength = maxClauseLength;
        fullReasonsList = new ArrayList<DNF<CoreLiteral>>();
        reasonQueueList = new ArrayList<Queue<DNF<CoreLiteral>>>();
    }



    public synchronized void addReason(DNF<CoreLiteral> reason, Queue myQueue){
        fullReasonsList.add(reason);
        for(Queue<DNF<CoreLiteral>> queue : reasonQueueList){
            if (queue != myQueue){
                queue.add(reason);
            }
        }
    }


}

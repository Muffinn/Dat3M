package com.dat3m.dartagnan.verification.solving;

import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.wmm.Relation;

import java.util.List;

public class MainStatisticManager {
    private List<Event> eventList;
    private List<Relation> relationList;
    private final ThreadStatisticManager[] statisticManagers;

    public MainStatisticManager(int numberOfSplits){
        statisticManagers = new ThreadStatisticManager[numberOfSplits];
        for (int i = 0; i < numberOfSplits; i++){
            statisticManagers[i] = new ThreadStatisticManager(i);
        }
    }

    public ThreadStatisticManager getStatisticManager(int threadID){
        return statisticManagers[threadID];
    }

    public void setEventList(List<Event> eventList) {
        this.eventList = eventList;
    }

    public void setRelationList(List<Relation> relationList) {
        this.relationList = relationList;
    }

    public void printThreadStatistics(){
        for(ThreadStatisticManager tSM: statisticManagers){
            tSM.print();
        }
    }
}

package com.dat3m.dartagnan.program.analysis;

import com.dat3m.dartagnan.program.Program;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.verification.Context;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface ExecutionAnalysis {

    boolean isImplied(Event start, Event implied);
    boolean areMutuallyExclusive(Event a, Event b);



    static ExecutionAnalysis fromConfig(Program program, Context context, Configuration config)
            throws InvalidConfigurationException {

        BranchEquivalence eq = context.requires(BranchEquivalence.class);
        return new ExecutionAnalysis() {
            @Override
            public boolean isImplied(Event start, Event implied) {
                return start == implied || (implied.cfImpliesExec() && eq.isImplied(start, implied));
            }

            @Override
            public boolean areMutuallyExclusive(Event a, Event b) {
                return eq.areMutuallyExclusive(a, b);
            }
        };
    }

    static ExecutionAnalysis withAssumptions(Program program, Context context, Configuration config, List<Event> executedRepresentatives, List<Event> nonExecutedRepresentatives)
            throws InvalidConfigurationException {
        ExecutionAnalysis exec = fromConfig(program, context, config);
        Set<Event> executedEvents = new HashSet<Event>();
        Set<Event> nonExecutedEvents = new HashSet<Event>();
        for(Event e : program.getEvents()) {
            if (executedRepresentatives.stream().anyMatch(r -> exec.isImplied(r, e))) {
                executedEvents.add(e);
            }
            if (nonExecutedRepresentatives.stream().anyMatch(r -> exec.isImplied(e, r))) {
                nonExecutedEvents.add(e);
            }
        }


        return new ExecutionAnalysis() {
            @Override
            public boolean isImplied(Event start, Event implied) {


                return executedEvents.contains(implied) || exec.isImplied(start, implied);
            }

            @Override
            public boolean areMutuallyExclusive(Event a, Event b) {

                return nonExecutedEvents.contains(a) || nonExecutedEvents.contains(b) || exec.areMutuallyExclusive(a, b);
            }
        };

    }
}

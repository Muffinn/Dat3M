package dartagnan.wmm.axiom;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Z3Exception;
import dartagnan.program.event.Event;
import dartagnan.utils.Utils;
import dartagnan.wmm.relation.Relation;
import dartagnan.wmm.utils.Tuple;
import dartagnan.wmm.utils.TupleSet;

import java.util.*;

import static dartagnan.utils.Utils.edge;

/**
 *
 * @author Florian Furbach
 */
public class Acyclic extends Axiom {

    public Acyclic(Relation rel) {
        super(rel);
    }

    public Acyclic(Relation rel, boolean negate) {
        super(rel, negate);
    }

    @Override
    public TupleSet getEncodeTupleSet(){
        Map<Event, Set<Event>> transMap = rel.getMaxTupleSet().transMap();
        TupleSet result = new TupleSet();

        for(Event e1 : transMap.keySet()){
            if(transMap.get(e1).contains(e1)){
                for(Event e2 : transMap.get(e1)){
                    if(!e2.getEId().equals(e1.getEId()) && transMap.get(e2).contains(e1)){
                        result.add(new Tuple(e1, e2));
                    }
                }
            }
        }

        for(Tuple tuple : rel.getMaxTupleSet()){
            if(tuple.getFirst().getEId().equals(tuple.getSecond().getEId())){
                result.add(tuple);
            }
        }

        result.retainAll(rel.getMaxTupleSet());
        return result;
    }

    @Override
    protected BoolExpr _consistent(Context ctx) throws Z3Exception {
        BoolExpr enc = ctx.mkTrue();
        for(Tuple tuple : rel.getEncodeTupleSet()){
            Event e1 = tuple.getFirst();
            Event e2 = tuple.getSecond();
            enc = ctx.mkAnd(enc, ctx.mkImplies(e1.executes(ctx), ctx.mkGt(Utils.intVar(rel.getName(), e1, ctx), ctx.mkInt(0))));
            enc = ctx.mkAnd(enc, ctx.mkImplies(Utils.edge(rel.getName(), e1, e2, ctx), ctx.mkLt(Utils.intVar(rel.getName(), e1, ctx), Utils.intVar(rel.getName(), e2, ctx))));
        }
        return enc;
    }

    @Override
    protected BoolExpr _inconsistent(Context ctx) throws Z3Exception {
        return ctx.mkAnd(satCycleDef(ctx), satCycle(ctx));
    }

    @Override
    protected String _toString() {
        return String.format("acyclic %s", rel.getName());
    }

    private BoolExpr satCycle(Context ctx) throws Z3Exception {
        Set<Event> cycleEvents = new HashSet<>();
        for(Tuple tuple : rel.getEncodeTupleSet()){
            cycleEvents.add(tuple.getFirst());
        }

        BoolExpr cycle = ctx.mkFalse();
        for(Event e : cycleEvents){
            cycle = ctx.mkOr(cycle, cycleVar(rel.getName(), e, ctx));
        }

        return cycle;
    }

    private BoolExpr satCycleDef(Context ctx){
        BoolExpr enc = ctx.mkTrue();
        Set<Event> encoded = new HashSet<>();
        String name = rel.getName();

        for(Tuple t : rel.getEncodeTupleSet()){
            Event e1 = t.getFirst();
            Event e2 = t.getSecond();

            enc = ctx.mkAnd(enc, ctx.mkImplies(
                    cycleEdge(name, e1, e2, ctx),
                    ctx.mkAnd(
                            e1.executes(ctx),
                            e2.executes(ctx),
                            edge(name, e1, e2, ctx),
                            cycleVar(name, e1, ctx),
                            cycleVar(name, e2, ctx)
            )));

            if(!encoded.contains(e1)){
                encoded.add(e1);

                BoolExpr source = ctx.mkFalse();
                for(Tuple tuple1 : rel.getEncodeTupleSet().getByFirst(e1)){
                    BoolExpr opt = cycleEdge(name, e1, tuple1.getSecond(), ctx);
                    for(Tuple tuple2 : rel.getEncodeTupleSet().getByFirst(e1)){
                        if(!tuple1.getSecond().getEId().equals(tuple2.getSecond().getEId())){
                            opt = ctx.mkAnd(opt, ctx.mkNot(cycleEdge(name, e1, tuple2.getSecond(), ctx)));
                        }
                    }
                    source = ctx.mkOr(source, opt);
                }

                BoolExpr target = ctx.mkFalse();
                for(Tuple tuple1 : rel.getEncodeTupleSet().getBySecond(e1)){
                    BoolExpr opt = cycleEdge(name, tuple1.getFirst(), e1, ctx);
                    for(Tuple tuple2 : rel.getEncodeTupleSet().getBySecond(e1)){
                        if(!tuple1.getFirst().getEId().equals(tuple2.getFirst().getEId())){
                            opt = ctx.mkAnd(opt, ctx.mkNot(cycleEdge(name, tuple2.getFirst(), e1, ctx)));
                        }
                    }
                    target = ctx.mkOr(target, opt);
                }

                enc = ctx.mkAnd(enc, ctx.mkImplies(cycleVar(name, e1, ctx), ctx.mkAnd(source, target)));
            }
        }

        return enc;
    }

    private BoolExpr cycleVar(String relName, Event e, Context ctx) throws Z3Exception {
        return ctx.mkBoolConst("Cycle(" + e.repr() + ")(" + relName + ")");
    }

    private BoolExpr cycleEdge(String relName, Event e1, Event e2, Context ctx) throws Z3Exception {
        return ctx.mkBoolConst("Cycle:" + relName + "(" + e1.repr() + "," + e2.repr() + ")");
    }
}

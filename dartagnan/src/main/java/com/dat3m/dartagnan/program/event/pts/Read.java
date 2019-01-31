package com.dat3m.dartagnan.program.event.pts;

import com.dat3m.dartagnan.expression.IExpr;
import com.dat3m.dartagnan.program.Register;
import com.dat3m.dartagnan.program.Seq;
import com.dat3m.dartagnan.program.event.Fence;
import com.dat3m.dartagnan.program.event.Load;
import com.dat3m.dartagnan.program.event.MemEvent;
import com.dat3m.dartagnan.program.event.utils.RegWriter;
import com.dat3m.dartagnan.program.utils.EType;
import com.dat3m.dartagnan.program.Thread;

public class Read extends MemEvent implements RegWriter {

    protected Register resultRegister;

    public Read(Register register, IExpr address, String atomic) {
        this.resultRegister = register;
        this.address = address;
        this.atomic = atomic;
        this.condLevel = 0;
        addFilters(EType.ANY, EType.MEMORY, EType.READ);
    }

    @Override
    public Register getResultRegister(){
        return resultRegister;
    }

    @Override
    public String toString() {
        return nTimesCondLevel() + resultRegister + " = load(*" + address + ", " + (atomic != null ? ", " + atomic : "") + ")";
    }

    @Override
    public Read clone() {
        if(clone == null){
            clone = new Read(resultRegister.clone(), address.clone(), atomic);
            afterClone();
        }
        return (Read)clone;
    }

    @Override
    public Thread compile(String target, boolean ctrl, boolean leading) {
        Load ld = new Load(resultRegister, address, atomic);
        ld.setHLId(hlId);
        ld.setCondLevel(this.condLevel);

        if(!target.equals("power") && !target.equals("arm")) {
            return ld;
        }

        if(atomic.equals("_rx") || atomic.equals("_na")) {
            return ld;
        }

        if(target.equals("power")) {
            Fence lwsync = new Fence("Lwsync");
            lwsync.setCondLevel(this.condLevel);
            if(atomic.equals("_con") || atomic.equals("_acq")) {
                return new Seq(ld, lwsync);
            }

            if(atomic.equals("_sc")) {
                if(leading) {
                    Fence sync = new Fence("Sync");
                    sync.setCondLevel(this.condLevel);
                    return new Seq(sync, new Seq(ld, lwsync));
                }
                return new Seq(ld, lwsync);
            }
        }

        if(target.equals("arm")) {
            if(atomic.equals("_con") || atomic.equals("_acq") || atomic.equals("_sc")) {
                Fence ish = new Fence("Ish");
                ish.setCondLevel(this.condLevel);
                return new Seq(ld, ish);
            }
        }

        throw new RuntimeException("Compilation is not supported for " + this);
    }
}

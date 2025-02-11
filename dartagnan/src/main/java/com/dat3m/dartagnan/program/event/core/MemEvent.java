package com.dat3m.dartagnan.program.event.core;

import com.dat3m.dartagnan.expression.ExprInterface;
import com.dat3m.dartagnan.expression.IExpr;
import com.dat3m.dartagnan.program.event.Tag.C11;
import com.dat3m.dartagnan.program.event.visitors.EventVisitor;
import com.google.common.base.Preconditions;

public abstract class MemEvent extends Event {

    protected IExpr address;
    protected String mo;

    public MemEvent(IExpr address, String mo){
        this.address = address;
        this.mo = mo;
        if(mo != null){
            addFilters(mo);
        }
    }
    
    protected MemEvent(MemEvent other){
        super(other);
        this.address = other.address;
        this.mo = other.mo;
    }

    public IExpr getAddress(){
        return address;
    }

    public void setAddress(IExpr address){
        this.address = address;
    }

    public ExprInterface getMemValue(){
        throw new RuntimeException("MemValue is not available for event " + this.getClass().getName());
    }
    
    public void setMemValue(ExprInterface value){
        throw new RuntimeException("SetValue is not available for event " + this.getClass().getName());
    }
    
    public String getMo(){
        return mo;
    }

    public void setMo(String mo){
    	Preconditions.checkNotNull(mo, "Only the parser can set the memory ordering to null");
    	if(this.mo != null) {
            removeFilters(this.mo);    		
    	}
        this.mo = mo;
        addFilters(mo);
    }

    public boolean canRace() {
    	return mo == null || mo.equals(C11.NONATOMIC);
    }

	// Visitor
	// -----------------------------------------------------------------------------------------------------------------

	@Override
	public <T> T accept(EventVisitor<T> visitor) {
		return visitor.visitMemEvent(this);
	}
}

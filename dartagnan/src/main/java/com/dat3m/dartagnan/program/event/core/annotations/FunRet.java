package com.dat3m.dartagnan.program.event.core.annotations;

import com.dat3m.dartagnan.program.event.visitors.EventVisitor;

public class FunRet extends CodeAnnotation {

	private final String funName;
	
	public FunRet(String funName) {
		this.funName = funName;
	}
	
	protected FunRet(FunRet other){
		super(other);
		this.funName = other.funName;
	}

    @Override
    public String toString(){
        return "=== Returning from " + funName + " ===";
    }

    public String getFunctionName() {
    	return funName;
    }

	// Unrolling
	// -----------------------------------------------------------------------------------------------------------------

	@Override
	public FunRet getCopy(){
		return new FunRet(this);
	}

	// Visitor
	// -----------------------------------------------------------------------------------------------------------------

	@Override
	public <T> T accept(EventVisitor<T> visitor) {
		return visitor.visitFunRet(this);
	}
}

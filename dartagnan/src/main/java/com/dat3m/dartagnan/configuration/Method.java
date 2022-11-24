package com.dat3m.dartagnan.configuration;

import java.util.Arrays;

public enum Method implements OptionInterface {
	ASSUME, INCREMENTAL, TWO, CAAT, PARALLEL_ASSUME, PARALLEL_CAAT;
	
	// Used for options in the console
	@Override
	public String asStringOption() {
        switch(this) {
        	case TWO:
        		return "two";
        	case INCREMENTAL:
        		return "incremental";
        	case ASSUME:
        		return "assume";
			case CAAT:
				return "caat";
			case PARALLEL_ASSUME:
				return "parallelassume";
			case PARALLEL_CAAT:
				return "parallelcaat";
        }
        throw new UnsupportedOperationException("Unrecognized analysis " + this);
	}

	// Used to display in UI
	@Override
	public String toString() {
        switch(this) {
			case TWO:
				return "Two Solvers";
			case INCREMENTAL:
				return "Incremental Solver";
			case ASSUME:
            	return "Solver with Assumption";
            case CAAT:
            	return "CAAT Solver";
			case PARALLEL_ASSUME:
				return "Parallel Solver with Assumption";
			case PARALLEL_CAAT:
				return "Parallel Caat Solver";
        }
        throw new UnsupportedOperationException("Unrecognized analysis " + this);
	}

	public static Method getDefault() {
		return PARALLEL_CAAT;
	}
	
	// Used to decide the order shown by the selector in the UI
	public static Method[] orderedValues() {
		Method[] order = { INCREMENTAL, ASSUME, TWO, CAAT, PARALLEL_ASSUME, PARALLEL_CAAT};
		// Be sure no element is missing
		assert(Arrays.asList(order).containsAll(Arrays.asList(values())));
		return order;
	}
}
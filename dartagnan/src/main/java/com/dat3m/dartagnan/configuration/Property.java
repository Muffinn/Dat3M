package com.dat3m.dartagnan.configuration;

import java.util.Arrays;

public enum Property implements OptionInterface {
	REACHABILITY, RACES, LIVENESS;
	
	// Used for options in the console
	public String asStringOption() {
        switch(this) {
			case REACHABILITY:
				return "reachability";
			case RACES:
				return "races";
			case LIVENESS:
				return "liveness";
			default:
				throw new UnsupportedOperationException("Unrecognized property: " + this);
		}
	}

	// Used to display in UI
    @Override
    public String toString() {
        switch(this){
        	case REACHABILITY:
        		return "Reachability";
        	case RACES:
        		return "Races";
			case LIVENESS:
				return "Liveness";
			default:
				throw new UnsupportedOperationException("Unrecognized property: " + this);
        }
    }

	public static Property getDefault() {
		return REACHABILITY;
	}

	// Used to decide the order shown by the selector in the UI
	public static Property[] orderedValues() {
		Property[] order = { REACHABILITY, RACES, LIVENESS };
		// Be sure no element is missing
		assert(Arrays.asList(order).containsAll(Arrays.asList(values())));
		return order;
	}
}
package com.tramchester.graph.search.stateMachine;

public class NextStateNotFoundException extends RuntimeException {
    public NextStateNotFoundException(RegistersStates.FromTo key) {
        super("No next state, attempted transition was " + key.toString());
    }
}

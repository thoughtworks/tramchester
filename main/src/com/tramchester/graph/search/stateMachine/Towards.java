package com.tramchester.graph.search.stateMachine;

import com.tramchester.graph.search.stateMachine.states.TraversalState;

public interface Towards<T extends TraversalState> {
    void register(RegistersFromState registers);
    Class<T> getDestination();
}

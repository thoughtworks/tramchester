package com.tramchester.graph.search.stateMachine;

import com.tramchester.graph.search.stateMachine.states.TraversalState;
import com.tramchester.graph.search.stateMachine.states.TraversalStateType;

public interface Towards<T extends TraversalState> {
    void register(RegistersFromState registers);
    TraversalStateType getDestination();
}

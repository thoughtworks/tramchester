package com.tramchester.graph.search.stateMachine;

import com.tramchester.graph.search.stateMachine.states.TraversalState;

public interface RegistersFromState {
    <T extends TraversalState> void add(Class<? extends TraversalState> from, TowardsState<T> builder);
}

package com.tramchester.graph.search.states;

public interface TowardsState<T extends TraversalState> {
    void register(RegistersFromState registers);
    Class<T> getDestination();
}

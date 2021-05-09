package com.tramchester.graph.search.states;

public interface RegistersFromState {
    <T extends TraversalState> void add(Class<? extends TraversalState> from, TowardsState<T> builder);
}

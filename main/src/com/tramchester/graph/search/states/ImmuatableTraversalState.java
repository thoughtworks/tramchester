package com.tramchester.graph.search.states;

import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.search.JourneyState;
import org.neo4j.graphdb.Node;

public interface ImmuatableTraversalState {
    int getTotalCost();
    TraversalState nextState(GraphBuilder.Labels nodeLabel, Node node,
                             JourneyState journeyState, int cost);
}

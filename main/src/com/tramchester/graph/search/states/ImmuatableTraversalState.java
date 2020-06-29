package com.tramchester.graph.search.states;

import com.tramchester.graph.build.GraphBuilder;
import com.tramchester.graph.search.JourneyState;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;

public interface ImmuatableTraversalState {
    int getTotalCost();
    TraversalState nextState(Path path, GraphBuilder.Labels nodeLabel, Node node,
                             JourneyState journeyState, int cost);
}

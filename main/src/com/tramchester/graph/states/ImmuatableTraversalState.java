package com.tramchester.graph.states;

import com.tramchester.graph.JourneyState;
import com.tramchester.graph.TransportGraphBuilder;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;

public interface ImmuatableTraversalState {
    int getTotalCost();
    TraversalState nextState(Path path, TransportGraphBuilder.Labels nodeLabel, Node node,
                                        JourneyState journeyState, int cost);
}

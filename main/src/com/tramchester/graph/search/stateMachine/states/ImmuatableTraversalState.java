package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.search.JourneyStateUpdate;
import org.neo4j.graphdb.Node;

import java.util.Set;

public interface ImmuatableTraversalState {
    int getTotalCost();
    TraversalState nextState(Set<GraphLabel> nodeLabels, Node node,
                             JourneyStateUpdate journeyState, int cost);
}

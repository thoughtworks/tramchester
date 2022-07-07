package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.search.JourneyStateUpdate;
import org.neo4j.graphdb.Node;

import java.time.Duration;
import java.util.Set;

public interface ImmuatableTraversalState {
    /***
     * Use getTotalDuration()
     */
    @Deprecated
    Duration getTotalCost();

    Duration getTotalDuration();

    TraversalState nextState(Set<GraphLabel> nodeLabels, Node node,
                             JourneyStateUpdate journeyState, Duration duration, boolean alreadyOnDiversion);
}

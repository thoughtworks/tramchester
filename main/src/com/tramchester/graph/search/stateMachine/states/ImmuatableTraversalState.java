package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.search.JourneyStateUpdate;
import org.neo4j.graphdb.Node;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;

public interface ImmuatableTraversalState {

    Duration getTotalDuration();

    TraversalState nextState(EnumSet<GraphLabel> nodeLabels, Node node,
                             JourneyStateUpdate journeyState, Duration duration, boolean alreadyOnDiversion);

    TraversalStateType getStateType();
}

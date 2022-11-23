package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.TraversalOps;
import org.neo4j.graphdb.Node;

import java.time.Duration;
import java.util.Set;

public class NotStartedState extends TraversalState {

    public NotStartedState(TraversalOps traversalOps, TraversalStateFactory traversalStateFactory, Set<TransportMode> requestedModes) {
        super(traversalOps, traversalStateFactory, requestedModes);
    }

    @Override
    public String toString() {
        return "NotStartedState{}";
    }

    @Override
    public Duration getTotalCost() {
        return Duration.ZERO;
    }

    @Override
    protected TraversalState toWalk(WalkingState.Builder towardsWalk, Node node, Duration cost, JourneyStateUpdate journeyState) {
        journeyState.beginWalk(node, true, cost);
        return towardsWalk.fromStart(this, node, cost);
    }

    @Override
    protected TraversalState toGrouped(GroupedStationState.Builder towardsGroup, Node node, Duration cost, JourneyStateUpdate journeyState) {
        return towardsGroup.fromStart(this, node, cost);
    }

    @Override
    protected PlatformStationState toTramStation(PlatformStationState.Builder towardsStation, Node node, Duration cost,
                                                 JourneyStateUpdate journeyState, boolean onDiversion) {
        return towardsStation.fromStart(this, node, cost, journeyState, onDiversion, onDiversion);
    }

    @Override
    protected TraversalState toNoPlatformStation(NoPlatformStationState.Builder towardsStation, Node node, Duration cost,
                                                 JourneyStateUpdate journeyState, boolean onDiversion) {
        return towardsStation.fromStart(this, node, cost, journeyState, onDiversion, onDiversion);
    }
}

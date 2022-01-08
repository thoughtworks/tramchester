package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.TraversalOps;
import org.neo4j.graphdb.Node;

public class NotStartedState extends TraversalState {

    public NotStartedState(TraversalOps traversalOps, TraversalStateFactory traversalStateFactory) {
        super(traversalOps, traversalStateFactory);
    }

    @Override
    public String toString() {
        return "NotStartedState{}";
    }

    @Override
    public int getTotalCost() {
        return 0;
    }

    @Override
    protected TraversalState toWalk(WalkingState.Builder towardsWalk, Node node, int cost, JourneyStateUpdate journeyState) {
        journeyState.beginWalk(node, true, cost);
        return towardsWalk.fromStart(this, node, cost);
    }

    @Override
    protected TraversalState toGrouped(GroupedStationState.Builder towardsGroup, Node node, int cost, JourneyStateUpdate journeyState) {
        return towardsGroup.fromStart(this, node, cost);
    }

    @Override
    protected PlatformStationState toTramStation(PlatformStationState.Builder towardsStation, Node node, int cost, JourneyStateUpdate journeyState) {
        return towardsStation.fromStart(this, node, cost, journeyState);
    }

    @Override
    protected TraversalState toNoPlatformStation(NoPlatformStationState.Builder towardsStation, Node node, int cost, JourneyStateUpdate journeyState) {
        return towardsStation.fromStart(this, node, cost, journeyState);
    }
}

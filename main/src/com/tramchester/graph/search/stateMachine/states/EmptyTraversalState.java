package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.Towards;
import org.neo4j.graphdb.Node;

import java.time.Duration;

public abstract class EmptyTraversalState {

    protected final TraversalStateType stateType;

    protected EmptyTraversalState(TraversalStateType stateType) {
        this.stateType = stateType;
    }

    protected JustBoardedState toJustBoarded(JustBoardedState.Builder towardsJustBoarded, Node node, Duration cost, JourneyStateUpdate journeyState) {
        throw new RuntimeException("No such transition at " + stateType);
    }

    protected TraversalState toWalk(WalkingState.Builder towardsWalk, Node node, Duration cost, JourneyStateUpdate journeyState) {
        throw new RuntimeException("No such transition at " + stateType);
    }

    protected TraversalState toPlatform(PlatformState.Builder towardsPlatform, Node node, Duration cost, JourneyStateUpdate journeyState) {
        throw new RuntimeException("No such transition at " + stateType);
    }

    protected TraversalState toService(ServiceState.Builder towardsService, Node node, Duration cost) {
        throw new RuntimeException("No such transition at " + stateType);
    }

    protected TraversalState toNoPlatformStation(NoPlatformStationState.Builder towardsNoPlatformStation, Node node, Duration cost,
                                                 JourneyStateUpdate journeyState, boolean onDiversion) {
        throw new RuntimeException("No such transition at " + stateType);
    }

    protected TraversalState toGrouped(GroupedStationState.Builder towardsGroup, Node node, Duration cost, JourneyStateUpdate journeyState) {
        throw new RuntimeException("No such transition at " + stateType);
    }

    protected TraversalState toMinute(MinuteState.Builder towardsMinute, Node node, Duration cost, JourneyStateUpdate journeyState, TransportRelationshipTypes[] currentModes) {
        throw new RuntimeException("No such transition at " + stateType);
    }

    protected PlatformStationState toPlatformStation(PlatformStationState.Builder towardsStation, Node node, Duration cost, JourneyStateUpdate journeyState, boolean onDiversion) {
        throw new RuntimeException("No such transition at " + stateType);
    }

    protected void toDestination(DestinationState.Builder towardsDestination, Node node, Duration cost, JourneyStateUpdate journeyStateUpdate) {
        throw new RuntimeException("No such transition at " + stateType);
    }

    protected HourState toHour(HourState.Builder towardsHour, Node node, Duration cost) {
        throw new RuntimeException("No such transition at " + stateType);
    }

    protected RouteStationStateOnTrip toRouteStationOnTrip(RouteStationStateOnTrip.Builder towardsRouteStation,
                                                           Node node, Duration cost, boolean isInterchange) {
        throw new RuntimeException("No such transition at " + stateType);
    }

    protected RouteStationStateEndTrip toRouteStationEndTrip(RouteStationStateEndTrip.Builder towardsRouteStation,
                                                             Node node, Duration cost, boolean isInterchange) {
        throw new RuntimeException("No such transition at " + stateType);
    }

}

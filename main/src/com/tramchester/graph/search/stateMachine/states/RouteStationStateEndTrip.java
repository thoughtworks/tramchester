package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.TowardsRouteStation;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

public class RouteStationStateEndTrip extends RouteStationState {

    @Override
    public String toString() {
        return "RouteStationStateEndTrip{" +
                "mode=" + mode +
                "} " + super.toString();
    }

    public static class Builder extends TowardsRouteStation<RouteStationStateEndTrip> {

        @Override
        public void register(RegistersFromState registers) {
            registers.add(MinuteState.class, this);
        }

        @Override
        public Class<RouteStationStateEndTrip> getDestination() {
            return RouteStationStateEndTrip.class;
        }

        public RouteStationStateEndTrip fromMinuteState(MinuteState minuteState, Node node, int cost,
                                                        Iterable<Relationship> routeStationOutbound) {
            TransportMode transportMode = GraphProps.getTransportMode(node);
            return new RouteStationStateEndTrip(minuteState, routeStationOutbound, cost, transportMode, node);
        }

    }

    private final TransportMode mode;
    private final Node routeStationNode;

    private RouteStationStateEndTrip(MinuteState minuteState, Iterable<Relationship> routeStationOutbound, int cost,
                                     TransportMode mode, Node routeStationNode) {
        super(minuteState, routeStationOutbound, cost);
        this.mode = mode;
        this.routeStationNode = routeStationNode;
    }

    @Override
    protected TraversalState toService(ServiceState.Builder towardsService, Node node, int cost) {
        return towardsService.fromRouteStation(this, node, cost);
    }

    @Override
    protected TraversalState toNoPlatformStation(NoPlatformStationState.Builder towardsStation, Node node, int cost, JourneyStateUpdate journeyState) {
        leaveVehicle(journeyState);
        return towardsStation.fromRouteStation(this, node, cost);
    }

    @Override
    protected TraversalState toPlatform(PlatformState.Builder towardsPlatform, Node node, int cost, JourneyStateUpdate journeyState) {
        leaveVehicle(journeyState);
        return towardsPlatform.fromRouteStatiomEndTrip(this, node, cost);
    }

    private void leaveVehicle(JourneyStateUpdate journeyState) {
        try {
            journeyState.leave(mode, getTotalCost(), routeStationNode);
        } catch (TramchesterException e) {
            throw new RuntimeException("Unable to leave " + mode, e);
        }
    }
}

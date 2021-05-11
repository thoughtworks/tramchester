package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.search.JourneyState;
import com.tramchester.graph.search.stateMachine.NodeId;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.TowardsRouteStation;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.Collection;

public class RouteStationStateOnTrip extends RouteStationState implements NodeId {
    private final long routeStationNodeId;
    private final IdFor<Trip> tripId;
    private final TransportMode transportMode;

    public static class Builder extends TowardsRouteStation<RouteStationStateOnTrip> {

        @Override
        public void register(RegistersFromState registers) {
            registers.add(MinuteState.class, this);
        }

        @Override
        public Class<RouteStationStateOnTrip> getDestination() {
            return RouteStationStateOnTrip.class;
        }

        public RouteStationStateOnTrip fromMinuteState(MinuteState minuteState, Node node, int cost,
                                                       Collection<Relationship> routeStationOutbound) {
            TransportMode transportMode = GraphProps.getTransportMode(node);
            return new RouteStationStateOnTrip(minuteState, routeStationOutbound, cost, node.getId(), minuteState.getTripId(), transportMode);
        }
    }

    private RouteStationStateOnTrip(TraversalState parent, Iterable<Relationship> relationships, int cost,
                                    long routeStationNodeId, IdFor<Trip> tripId, TransportMode transportMode) {
        super(parent, relationships, cost);
        this.routeStationNodeId = routeStationNodeId;
        this.tripId = tripId;
        this.transportMode = transportMode;
    }

    @Override
    public String toString() {
        return "RouteStationStateOnTrip{" +
                "routeStationNodeId=" + routeStationNodeId +
                ", tripId=" + tripId +
                ", transportMode=" + transportMode +
                "} " + super.toString();
    }

    @Override
    protected TraversalState toService(ServiceState.Builder towardsService, Node node, int cost) {
        return towardsService.fromRouteStation(this, tripId, node, cost);
    }

    @Override
    protected TraversalState toNoPlatformStation(NoPlatformStationState.Builder towardsNoPlatformStation, Node node, int cost, JourneyState journeyState) {
        leaveVehicle(journeyState, transportMode, "Unable to depart tram");
        return towardsNoPlatformStation.fromRouteStation(this, node, cost);
    }

    @Override
    protected TraversalState toPlatform(PlatformState.Builder towardsPlatform, Node node, int cost, JourneyState journeyState) {
        leaveVehicle(journeyState, TransportMode.Tram, "Unable to process platform");
        return towardsPlatform.fromRouteStationOnTrip(this, node, cost);
    }

    private void leaveVehicle(JourneyState journeyState, TransportMode transportMode, String s) {
        try {
            journeyState.leave(transportMode, getTotalCost());
        } catch (TramchesterException e) {
            throw new RuntimeException(s, e);
        }
    }

    @Override
    public long nodeId() {
        return routeStationNodeId;
    }

}

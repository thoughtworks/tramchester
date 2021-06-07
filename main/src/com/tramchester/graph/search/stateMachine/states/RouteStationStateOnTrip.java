package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.Service;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.NodeId;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.TowardsRouteStation;
import com.tramchester.graph.search.stateMachine.TraversalOps;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.ArrayList;
import java.util.List;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static com.tramchester.graph.TransportRelationshipTypes.INTERCHANGE_DEPART;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class RouteStationStateOnTrip extends RouteStationState implements NodeId {
    private final IdFor<Trip> tripId;
    private final TransportMode transportMode;
    private final Node routeStationNode;

    public static class Builder extends TowardsRouteStation<RouteStationStateOnTrip> {

        private final boolean interchangesOnly;

        public Builder(boolean interchangesOnly) {
            this.interchangesOnly = interchangesOnly;
        }

        @Override
        public void register(RegistersFromState registers) {
            registers.add(MinuteState.class, this);
        }

        @Override
        public Class<RouteStationStateOnTrip> getDestination() {
            return RouteStationStateOnTrip.class;
        }

        public RouteStationStateOnTrip fromMinuteState(MinuteState minuteState, Node node, int cost, boolean isInterchange, Trip trip) {
            TransportMode transportMode = GraphProps.getTransportMode(node);

            Iterable<Relationship> allDeparts = node.getRelationships(OUTGOING, DEPART, INTERCHANGE_DEPART);

            List<Relationship> towardsDestination = minuteState.traversalOps.getTowardsDestination(allDeparts);
            if (!towardsDestination.isEmpty()) {
                // we've nearly arrived
                return new RouteStationStateOnTrip(minuteState, towardsDestination, cost, node, trip.getId(), transportMode);
            }

            // outbound service relationships that continue the current trip
            List<Relationship> towardsServiceForTrip = filterByTripId(node.getRelationships(OUTGOING, TO_SERVICE),
                    minuteState.traversalOps, trip);
            List<Relationship> outboundsToFollow = new ArrayList<>(towardsServiceForTrip);

            // now add outgoing to platforms/stations
            if (interchangesOnly) {
                if (isInterchange) {
                    Iterable<Relationship> interchanges = node.getRelationships(OUTGOING, INTERCHANGE_DEPART);
                    interchanges.forEach(outboundsToFollow::add);
                } // else add none
            } else {
                allDeparts.forEach(outboundsToFollow::add);
            }

            return new RouteStationStateOnTrip(minuteState, outboundsToFollow, cost, node, trip.getId(), transportMode);
        }

        private List<Relationship> filterByTripId(Iterable<Relationship> svcRelationships, TraversalOps traversalOps, Trip trip) {
            IdFor<Service> currentSvcId = trip.getService().getId();
            return traversalOps.filterByServiceId(svcRelationships, currentSvcId);
        }
    }

    private RouteStationStateOnTrip(TraversalState parent, Iterable<Relationship> relationships, int cost,
                                    Node routeStationNode, IdFor<Trip> tripId, TransportMode transportMode) {
        super(parent, relationships, cost);
        this.routeStationNode = routeStationNode;
        this.tripId = tripId;
        this.transportMode = transportMode;
    }

    @Override
    protected TraversalState toService(ServiceState.Builder towardsService, Node node, int cost) {
        return towardsService.fromRouteStation(this, tripId, node, cost);
    }

    @Override
    protected TraversalState toNoPlatformStation(NoPlatformStationState.Builder towardsNoPlatformStation, Node node, int cost,
                                                 JourneyStateUpdate journeyState) {
        leaveVehicle(journeyState, transportMode, "Unable to depart tram");
        return towardsNoPlatformStation.fromRouteStation(this, node, cost);
    }

    @Override
    protected TraversalState toPlatform(PlatformState.Builder towardsPlatform, Node node, int cost, JourneyStateUpdate journeyState) {
        leaveVehicle(journeyState, TransportMode.Tram, "Unable to process platform");
        return towardsPlatform.fromRouteStationOnTrip(this, node, cost);
    }

    private void leaveVehicle(JourneyStateUpdate journeyState, TransportMode transportMode, String diag) {
        try {
            journeyState.leave(transportMode, getTotalCost(), routeStationNode);
        } catch (TramchesterException e) {
            throw new RuntimeException(diag, e);
        }
    }

    @Override
    public long nodeId() {
        return routeStationNode.getId();
    }

    @Override
    public String toString() {
        return "RouteStationStateOnTrip{" +
                "routeStationNodeId=" + routeStationNode.getId() +
                ", tripId=" + tripId +
                ", transportMode=" + transportMode +
                "} " + super.toString();
    }


}

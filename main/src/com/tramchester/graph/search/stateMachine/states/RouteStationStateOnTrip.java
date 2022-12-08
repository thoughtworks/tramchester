package com.tramchester.graph.search.stateMachine.states;

import com.google.common.collect.Streams;
import com.tramchester.domain.Service;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.NodeId;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.TowardsRouteStation;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.TO_SERVICE;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class RouteStationStateOnTrip extends RouteStationState implements NodeId {

    private final IdFor<Trip> tripId;
    private final TransportMode transportMode;
    private final Node routeStationNode;

    @Override
    public TraversalStateType getStateType() {
        return TraversalStateType.RouteStationStateOnTrip;
    }

    public static class Builder extends TowardsRouteStation<RouteStationStateOnTrip> {

        private final NodeContentsRepository nodeContents;

        public Builder(boolean interchangesOnly, NodeContentsRepository nodeContents) {
            super(interchangesOnly);
            this.nodeContents = nodeContents;
        }

        @Override
        public void register(RegistersFromState registers) {
            registers.add(TraversalStateType.MinuteState, this);
        }

        @Override
        public TraversalStateType getDestination() {
            return TraversalStateType.RouteStationStateOnTrip;
        }

        public RouteStationStateOnTrip fromMinuteState(MinuteState minuteState, Node node, Duration cost,
                                                       boolean isInterchange, Trip trip) {
            // todo, use label and/or cache this - perf impact currently low
            TransportMode transportMode = GraphProps.getTransportMode(node);

            // TODO Crossing midnight?
            TramDate date = minuteState.traversalOps.getQueryDate();

            List<Relationship> towardsDestination = getTowardsDestination(minuteState.traversalOps, node, date);
            if (!towardsDestination.isEmpty()) {
                // we've nearly arrived
                return new RouteStationStateOnTrip(minuteState, towardsDestination.stream(), cost, node, trip.getId(), transportMode);
            }

            // outbound service relationships that continue the current trip
            Stream<Relationship> towardsServiceForTrip = filterByTripId(node.getRelationships(OUTGOING, TO_SERVICE), trip);

            // now add outgoing to platforms/stations
            Stream<Relationship> outboundsToFollow = getOutboundsToFollow(node, isInterchange, date);

            // NOTE: order of the concatenation matters here for depth first, need to do departs first to
            // explore routes including changes over continuing on possibly much longer trip
            final Stream<Relationship> relationships = Stream.concat(outboundsToFollow, towardsServiceForTrip);
            return new RouteStationStateOnTrip(minuteState, relationships, cost, node, trip.getId(), transportMode);
        }

        private Stream<Relationship> filterByTripId(Iterable<Relationship> svcRelationships, Trip trip) {
            IdFor<Service> currentSvcId = trip.getService().getId();
            return Streams.stream(svcRelationships).
                    filter(relationship -> currentSvcId.equals(nodeContents.getServiceId(relationship.getEndNode())));
        }

    }

    private RouteStationStateOnTrip(TraversalState parent, Stream<Relationship> relationships, Duration cost,
                                    Node routeStationNode, IdFor<Trip> tripId, TransportMode transportMode) {
        super(parent, relationships, cost);
        this.routeStationNode = routeStationNode;
        this.tripId = tripId;
        this.transportMode = transportMode;
    }

    @Override
    protected TraversalState toService(ServiceState.Builder towardsService, Node node, Duration cost) {
        return towardsService.fromRouteStation(this, tripId, node, cost);
    }

    @Override
    protected TraversalState toNoPlatformStation(NoPlatformStationState.Builder towardsNoPlatformStation, Node node, Duration cost,
                                                 JourneyStateUpdate journeyState, boolean onDiversion) {
        leaveVehicle(journeyState, transportMode, "Unable to depart tram");
        return towardsNoPlatformStation.fromRouteStation(this, node, cost, journeyState);
    }

    @Override
    protected TraversalState toPlatform(PlatformState.Builder towardsPlatform, Node node, Duration cost,
                                        JourneyStateUpdate journeyState) {

        leaveVehicle(journeyState, transportMode, "Unable to process platform");
        return towardsPlatform.fromRouteStationOnTrip(this, node, cost);
    }

    private void leaveVehicle(JourneyStateUpdate journeyState, TransportMode transportMode, String diag) {
        try {
            journeyState.leave(tripId, transportMode, getTotalDuration(), routeStationNode);
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

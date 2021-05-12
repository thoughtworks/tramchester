package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.InvalidId;
import com.tramchester.domain.input.Trip;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.ExistingTrip;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.Towards;
import org.geotools.data.store.EmptyIterator;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.ArrayList;
import java.util.List;

import static com.tramchester.graph.GraphPropertyKey.TRIP_ID;
import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class MinuteState extends TraversalState {

    public static class Builder implements Towards<MinuteState> {

        private final TramchesterConfig config;

        public Builder(TramchesterConfig config) {
            this.config = config;
        }

        @Override
        public void register(RegistersFromState registers) {
            registers.add(HourState.class, this);
        }

        @Override
        public Class<MinuteState> getDestination() {
            return MinuteState.class;
        }

        public TraversalState fromHour(HourState hourState, Node node, int cost, ExistingTrip existingTrip, JourneyStateUpdate journeyState) {
            Iterable<Relationship> relationships = node.getRelationships(OUTGOING, TRAM_GOES_TO, BUS_GOES_TO, TRAIN_GOES_TO
                ,FERRY_GOES_TO, SUBWAY_GOES_TO);

            boolean changeAtInterchangeOnly = config.getChangeAtInterchangeOnly();
            if (existingTrip.isOnTrip()) {
                IdFor<Trip> existingTripId = existingTrip.getTripId();
                Iterable<Relationship> filterBySingleTripId =
                        hourState.traversalOps.filterBySingleTripId(relationships, existingTripId);
                return new MinuteState(hourState, filterBySingleTripId, existingTripId, cost, changeAtInterchangeOnly);
            } else {
                // starting a brand new journey
                IdFor<Trip> newTripId = getTrip(node);
                journeyState.beginTrip(newTripId);
                return new MinuteState(hourState, relationships, newTripId, cost, changeAtInterchangeOnly);
            }
        }
    }

    private final boolean interchangesOnly;
    private final Trip trip;

    private MinuteState(TraversalState parent, Iterable<Relationship> relationships, IdFor<Trip> tripId, int cost, boolean interchangesOnly) {
        super(parent, relationships, cost);
        this.trip = traversalOps.getTrip(tripId);
        this.interchangesOnly = interchangesOnly;
    }

    private static IdFor<Trip> getTrip(Node endNode) {
        if (!GraphProps.hasProperty(TRIP_ID, endNode)) {
            return new InvalidId<>();
        }
        return GraphProps.getTripId(endNode);
    }

    public IdFor<Service> getServiceId() {
        return trip.getService().getId();
    }

    public IdFor<Trip> getTripId() {
        return trip.getId();
    }

    @Override
    protected RouteStationStateOnTrip toRouteStationOnTrip(RouteStationStateOnTrip.Builder towardsRouteStation,
                                                           Node routeStationNode, int cost, boolean isInterchange) {

        // outbound service relationships that continue the current trip
        List<Relationship> towardsServiceForTrip = filterByTripId(routeStationNode.getRelationships(OUTGOING, TO_SERVICE));
        List<Relationship> outboundsToFollow = new ArrayList<>(towardsServiceForTrip);

        // now add outgoing to platforms/stations
        if (interchangesOnly) {
            if (isInterchange) {
                Iterable<Relationship> interchanges = routeStationNode.getRelationships(OUTGOING, INTERCHANGE_DEPART);
                interchanges.forEach(outboundsToFollow::add);
            }
        } else {
            Iterable<Relationship> allDeparts = routeStationNode.getRelationships(OUTGOING, DEPART, INTERCHANGE_DEPART);
            allDeparts.forEach(outboundsToFollow::add);
        }

        return towardsRouteStation.fromMinuteState(this, routeStationNode, cost, outboundsToFollow);
    }

    @Override
    protected RouteStationStateEndTrip toRouteStationEndTrip(RouteStationStateEndTrip.Builder towardsRouteStation, Node routeStationNode,
                                                             int cost, boolean isInterchange) {
        Iterable<Relationship> outboundsToFollow;
        if (interchangesOnly) {
            if (isInterchange) {
                outboundsToFollow = routeStationNode.getRelationships(OUTGOING, INTERCHANGE_DEPART);
            } else {
                outboundsToFollow = EmptyIterator::new;
            }
        } else {
            outboundsToFollow = routeStationNode.getRelationships(OUTGOING, DEPART, INTERCHANGE_DEPART);
        }

        return towardsRouteStation.fromMinuteState(this, routeStationNode, cost, outboundsToFollow);
    }

    @Override
    protected RouteStationState toRouteStationTowardsDest(RouteStationStateEndTrip.Builder towardsRouteStation, Node node, int cost) {
        Iterable<Relationship> outboundsToFollow = node.getRelationships(OUTGOING, DEPART, INTERCHANGE_DEPART);
        return towardsRouteStation.fromMinuteState(this, node, cost, outboundsToFollow);
    }

    private List<Relationship> filterByTripId(Iterable<Relationship> svcRelationships) {
        IdFor<Service> currentSvcId = trip.getService().getId();
        return traversalOps.filterByServiceId(svcRelationships, currentSvcId);
    }

    @Override
    public String toString() {
        return "MinuteState{" +
                "interchangesOnly=" + interchangesOnly +
                ", trip='" + trip.getId() + '\'' +
                "} " + super.toString();
    }

}

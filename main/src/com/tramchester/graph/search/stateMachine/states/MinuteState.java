package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.InvalidId;
import com.tramchester.domain.input.Trip;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.search.JourneyState;
import com.tramchester.graph.search.stateMachine.ExistingTrip;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.Towards;
import com.tramchester.graph.search.stateMachine.UnexpectedNodeTypeException;
import org.geotools.data.store.EmptyIterator;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.tramchester.graph.GraphPropertyKey.TRIP_ID;
import static com.tramchester.graph.TransportRelationshipTypes.*;
import static com.tramchester.graph.graphbuild.GraphBuilder.Labels.INTERCHANGE;
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

        public TraversalState fromHour(HourState hourState, Node node, int cost, ExistingTrip existingTrip) {
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
    public TraversalState createNextState(Set<GraphBuilder.Labels> nodeLabels, Node node, JourneyState journeyState, int cost) {
//        // route station nodes may also have INTERCHANGE label set
//        if (nodeLabels.contains(GraphBuilder.Labels.ROUTE_STATION)) {
//            return toRouteStation(node, cost);
//        }
        throw new UnexpectedNodeTypeException(node, "Unexpected node types: "+nodeLabels);
    }

    @Override
    public TraversalState createNextState(GraphBuilder.Labels nodeLabel, Node node, JourneyState journeyState, int cost) {
        throw new UnexpectedNodeTypeException(node, "Unexpected node type: "+nodeLabel);
    }

    private TraversalState toRouteStation(Node routeStationNode, int cost) {
        Iterable<Relationship> allDeparts = routeStationNode.getRelationships(OUTGOING, DEPART, INTERCHANGE_DEPART);

        // if towards dest then always follow whether interchange-only enabled or not
        List<Relationship> towardsDestination = traversalOps.getTowardsDestination(allDeparts);
        if (!towardsDestination.isEmpty()) {
            // we've nearly arrived
            return builders.towardsRouteStateOnTrip(this).
                    fromMinuteState(this, routeStationNode, cost, towardsDestination);
        }

        List<Relationship> outboundsToFollow = new ArrayList<>();

        // outbound service relationships that continue the current trip
        List<Relationship> towardsServiceForTrip = filterByTripId(routeStationNode.getRelationships(OUTGOING, TO_SERVICE));
        boolean tripFinishedHere = towardsServiceForTrip.isEmpty(); // i.e. no outbound from RS for this tripId
        if (!tripFinishedHere) {
            outboundsToFollow.addAll(towardsServiceForTrip);
        }

        // now add outgoing to platforms/stations
        if (interchangesOnly) {
            if (routeStationNode.hasLabel(INTERCHANGE)) {
                Iterable<Relationship> interchanges = routeStationNode.getRelationships(OUTGOING, INTERCHANGE_DEPART);
                interchanges.forEach(outboundsToFollow::add);
            }
        } else {
            allDeparts.forEach(outboundsToFollow::add);
        }

        if (tripFinishedHere) {
            // for a change of trip id we need to get off vehicle, then back on to another service
            return builders.towardsRouteStateEndTrip(this).
                    fromMinuteState(this, routeStationNode, cost, outboundsToFollow);
        } else {
            return builders.towardsRouteStateOnTrip(this).
                    fromMinuteState(this, routeStationNode, cost, outboundsToFollow);
        }
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

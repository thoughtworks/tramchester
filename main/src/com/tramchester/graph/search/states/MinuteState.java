package com.tramchester.graph.search.states;

import com.tramchester.graph.NodeContentsRepository;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.search.JourneyState;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.tramchester.graph.GraphStaticKeys.TRIP_ID;
import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class MinuteState extends TraversalState {

    public static class Builder {

        public TraversalState fromHour(HourState hourState, Node node, int cost, ExistingTrip maybeExistingTrip) {
            Iterable<Relationship> relationships = node.getRelationships(OUTGOING, TRAM_GOES_TO, BUS_GOES_TO);

            if (maybeExistingTrip.isOnTrip()) {
                String existingTripId = maybeExistingTrip.getTripId();
                Iterable<Relationship> filterBySingleTripId = filterBySingleTripId(hourState.nodeOperations,
                        relationships, existingTripId);
                return new MinuteState(hourState, filterBySingleTripId, existingTripId, cost);
            } else {
                // starting a brand new journey
                String newTripId = getTrip(node);
                return new MinuteState(hourState, relationships, newTripId, cost);
            }
        }
    }

    private final String tripId;
    private final RouteStationState.Builder routeStationStateBuilder;
    private final RouteStationStateEndTrip.Builder routeStationStateEndTripBuilder;

    private MinuteState(TraversalState parent, Iterable<Relationship> relationships, String tripId, int cost) {
        super(parent, relationships, cost);
        this.tripId = tripId;
        routeStationStateBuilder = new RouteStationState.Builder();
        routeStationStateEndTripBuilder = new RouteStationStateEndTrip.Builder();
    }

    @Override
    public String toString() {
        return "MinuteState{" +
                "tripId='" + tripId + '\'' +
                ", parent=" + parent +
                '}';
    }

    private static String getTrip(Node endNode) {
        if (!endNode.hasProperty(TRIP_ID)) {
            return "";
        }
        return endNode.getProperty(TRIP_ID).toString().intern();
    }

    @Override
    public TraversalState createNextState(Path path, GraphBuilder.Labels nodeLabel, Node node, JourneyState journeyState, int cost) {
        if (nodeLabel == GraphBuilder.Labels.ROUTE_STATION) {
            return toRouteStation(node, cost);
        }
        throw new RuntimeException("Unexpected node type: "+nodeLabel);
    }

    private TraversalState toRouteStation(Node node, int cost) {
        Iterable<Relationship> allDeparts = node.getRelationships(OUTGOING, DEPART, INTERCHANGE_DEPART);

        // towards final destination, just follow this one
        for (Relationship depart : allDeparts) {
            if (destinationStationIds.contains(depart.getProperty(GraphStaticKeys.STATION_ID).toString())) {
                // we've arrived
                return routeStationStateBuilder.fromMinuteState(this, node, cost,
                        Collections.singleton(depart), tripId);
            }
        }

        List<Relationship> routeStationOutbound = filterByTripId(node.getRelationships(OUTGOING, TO_SERVICE), tripId);
        boolean tripFinishedHere = routeStationOutbound.isEmpty();

        // add outgoing to platforms
        if (config.getChangeAtInterchangeOnly()) {
            Iterable<Relationship> interchanges = node.getRelationships(OUTGOING, INTERCHANGE_DEPART);
            interchanges.forEach(routeStationOutbound::add);
        } else {
            allDeparts.forEach(routeStationOutbound::add);
        }

        if (tripFinishedHere) {
            // service finished here so don't pass in trip ID
            return routeStationStateEndTripBuilder.fromMinuteState(this, cost, routeStationOutbound);
        } else {
            return routeStationStateBuilder.fromMinuteState(this, node, cost, routeStationOutbound, tripId);
        }
    }

    private List<Relationship> filterByTripId(Iterable<Relationship> relationships, String tripId) {
        List<Relationship> results = new ArrayList<>();
        relationships.forEach(relationship -> {
            String trips = nodeOperations.getTrips(relationship);
            if (trips.contains(tripId)) {
                results.add(relationship);
            }
        });
        return results;
    }

    private static List<Relationship> filterBySingleTripId(NodeContentsRepository nodeOperations,
                                                           Iterable<Relationship> relationships, String tripId) {
        List<Relationship> results = new ArrayList<>();
        relationships.forEach(relationship -> {
            String trip = nodeOperations.getTrip(relationship);
            if (trip.equals(tripId)) {
                results.add(relationship);
            }
        });
        return results;
    }


}

package com.tramchester.graph.states;

import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.JourneyState;
import com.tramchester.graph.TransportGraphBuilder;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class MinuteState extends TraversalState {
    private static final Logger logger = LoggerFactory.getLogger(MinuteState.class);

    private final String tripId;

    @Override
    public String toString() {
        return "MinuteState{" +
                "tripId='" + tripId + '\'' +
                ", parent=" + parent +
                '}';
    }

    public MinuteState(TraversalState parent, Iterable<Relationship> relationships, String tripId, int cost) {
        super(parent, relationships, cost);
        this.tripId = tripId;
    }

    @Override
    public TraversalState nextState(Path path, TransportGraphBuilder.Labels nodeLabel, Node node, JourneyState journeyState, int cost) {
        if (nodeLabel == TransportGraphBuilder.Labels.ROUTE_STATION) {
            return toRouteStation(node, cost);
        }
        throw new RuntimeException("Unexpected node type: "+nodeLabel);
    }

    private TraversalState toRouteStation(Node node, int cost) {
        Iterable<Relationship> allDeparts = node.getRelationships(OUTGOING, DEPART, INTERCHANGE_DEPART);

        // towards final destination, just follow this one
        for (Relationship depart : allDeparts) {
            if (depart.getProperty(GraphStaticKeys.STATION_ID).equals(destinationStationdId)) {
                return new RouteStationState(this,
                        Collections.singleton(depart), node.getId(), tripId, cost);
            }
        }

        List<Relationship> routeStationOutbound = filterByTripId(node.getRelationships(OUTGOING, TO_SERVICE), tripId);
        boolean tripFinishedHere = routeStationOutbound.isEmpty();

        // add outgoing to platforms
        if (interchangesOnly) {
            Iterable<Relationship> interchanges = node.getRelationships(OUTGOING, INTERCHANGE_DEPART);
            interchanges.forEach(routeStationOutbound::add);
        } else {
            allDeparts.forEach(routeStationOutbound::add);
        }

        if (tripFinishedHere) {
            // service finished here so don't pass in trip ID
//            logger.info("No matching trips found for tripId " + tripId);
            return new RouteStationState(this, routeStationOutbound, node.getId(), cost, false);
        } else {
            return new RouteStationState(this, routeStationOutbound, node.getId(), tripId, cost);
        }
    }

    protected List<Relationship> filterByTripId(Iterable<Relationship> relationships, String tripId) {
        List<Relationship> results = new ArrayList<>();
        relationships.forEach(relationship -> {
            String trips = nodeOperations.getTrips(relationship);
            if (trips.contains(tripId)) {
                results.add(relationship);
            }
        });
        return results;
    }

}

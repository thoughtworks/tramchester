package com.tramchester.graph.states;

import com.tramchester.graph.CachedNodeOperations;
import com.tramchester.graph.JourneyState;
import com.tramchester.graph.TransportGraphBuilder;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import java.util.ArrayList;
import java.util.List;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class MinuteState extends TraversalState {
    private final String tripId;

    @Override
    public String toString() {
        return "MinuteState{" +
                "tripId='" + tripId + '\'' +
                ", parent=" + parent +
                '}';
    }

    public MinuteState(TraversalState parent, CachedNodeOperations nodeOperations, Iterable<Relationship> relationships,
                       String tripId, long destinationNodeId) {
        super(parent, nodeOperations, relationships, destinationNodeId);
        this.tripId = tripId;
    }

    @Override
    public TraversalState nextState(Path path, TransportGraphBuilder.Labels nodeLabel, Node node, JourneyState journeyState) {
        if (nodeLabel == TransportGraphBuilder.Labels.ROUTE_STATION) {
            Iterable<Relationship> departs = node.getRelationships(OUTGOING, DEPART, INTERCHANGE_DEPART);
            List<Relationship> outgoing = filterByTripId(node.getRelationships(OUTGOING, TO_SERVICE));
            departs.forEach(depart->outgoing.add(depart));

            return new RouteStationState(this, nodeOperations, outgoing, node.getId(), tripId, destinationNodeId);
        }
        throw new RuntimeException("Unexpected node type: "+nodeLabel);
    }

    private List<Relationship> filterByTripId(Iterable<Relationship> relationships) {
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

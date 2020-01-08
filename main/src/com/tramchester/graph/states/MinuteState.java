package com.tramchester.graph.states;

import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.JourneyState;
import com.tramchester.graph.TransportGraphBuilder;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import java.util.Collections;
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

        List<Relationship> outgoing = filterByTripId(node.getRelationships(OUTGOING, TO_SERVICE), tripId);

        if (interchangesOnly) {
            Iterable<Relationship> interchanges = node.getRelationships(OUTGOING, INTERCHANGE_DEPART);
            interchanges.forEach(outgoing::add);
        } else {
            allDeparts.forEach(outgoing::add);
        }

        return new RouteStationState(this, outgoing, node.getId(), tripId, cost);
    }

}

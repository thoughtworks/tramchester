package com.tramchester.graph.states;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.CachedNodeOperations;
import com.tramchester.graph.JourneyState;
import com.tramchester.graph.TransportGraphBuilder;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class RouteStationState extends TraversalState {
    private final long routeStationNodeId;
    private final boolean justBoarded;
    private final String tripId;

    public RouteStationState(TraversalState parent, CachedNodeOperations nodeOperations, Iterable<Relationship> relationships,
                             long routeStationNodeId, long destinationNodeId) {
        super(parent, nodeOperations, relationships, destinationNodeId);
        this.routeStationNodeId = routeStationNodeId;
        this.justBoarded = true;
        tripId = null;
    }

    public RouteStationState(TraversalState parent, CachedNodeOperations nodeOperations, Iterable<Relationship> relationships,
                             long routeStationNodeId, String tripId, long destinationNodeId) {
        super(parent, nodeOperations, relationships, destinationNodeId);
        this.routeStationNodeId = routeStationNodeId;
        this.justBoarded = false;
        this.tripId = tripId;
    }

    @Override
    public String toString() {
        return "RouteStationState{" +
                "routeStationNodeId=" + routeStationNodeId +
                ", justBoarded=" + justBoarded +
                ", tripId='" + tripId + '\'' +
                ", parent=" + parent +
                '}';
    }

    @Override
    public TraversalState nextState(Path path, TransportGraphBuilder.Labels nodeLabel, Node node, JourneyState journeyState) {
        try {
            if (nodeLabel == TransportGraphBuilder.Labels.PLATFORM) {
                journeyState.leaveTram(getTotalCost(path));
                Iterable<Relationship> relationships = node.getRelationships(OUTGOING, BOARD, INTERCHANGE_BOARD, LEAVE_PLATFORM);
                return new PlatformState(this, nodeOperations,
                        filterByEndNode(relationships, routeStationNodeId), node.getId(), destinationNodeId);
            }
        }
        catch (TramchesterException exception) {
            throw new RuntimeException("Unable to process platform", exception);
        }

        if (nodeLabel == TransportGraphBuilder.Labels.SERVICE) {
            return new ServiceState(this, nodeOperations, hourOrdered(node.getRelationships(OUTGOING, TO_HOUR)), destinationNodeId);
        }
        throw new RuntimeException("Unexpected node type: "+nodeLabel);
    }
}

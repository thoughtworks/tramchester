package com.tramchester.graph.states;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.CachedNodeOperations;
import com.tramchester.graph.JourneyState;
import com.tramchester.graph.TransportGraphBuilder;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import static com.tramchester.graph.TransportRelationshipTypes.ENTER_PLATFORM;
import static com.tramchester.graph.TransportRelationshipTypes.TO_SERVICE;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class PlatformState extends TraversalState {
    private final long platformNodeId;

    @Override
    public String toString() {
        return "PlatformState{" +
                "platformNodeId=" + platformNodeId +
                ", parent=" + parent +
                '}';
    }

    public PlatformState(TraversalState parent, CachedNodeOperations nodeOperations, Iterable<Relationship> relationships,
                         long platformNodeId, long destinationNodeId) {
        super(parent, nodeOperations, relationships, destinationNodeId);
        this.platformNodeId = platformNodeId;
    }

    @Override
    public TraversalState nextState(Path path, TransportGraphBuilder.Labels nodeLabel, Node node, JourneyState journeyState) {
        if (journeyState.isOnTram()) {
            throw new RuntimeException("Still on a tram! State: " + this.toString());
        }

        if (nodeLabel == TransportGraphBuilder.Labels.STATION) {
            return new StationState(this, nodeOperations, filterByEndNode(node.getRelationships(OUTGOING, ENTER_PLATFORM),
                    platformNodeId), destinationNodeId);
        }
        if (nodeLabel == TransportGraphBuilder.Labels.ROUTE_STATION) {
            try {
                journeyState.boardTram();
            } catch (TramchesterException e) {
                throw new RuntimeException("unable to board tram", e);
            }
            return new RouteStationState(this, nodeOperations, node.getRelationships(OUTGOING, TO_SERVICE), node.getId(),
                    destinationNodeId);
        }
        throw new RuntimeException("Unexpected node type: "+nodeLabel);
    }
}

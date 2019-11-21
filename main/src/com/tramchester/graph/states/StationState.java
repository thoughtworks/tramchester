package com.tramchester.graph.states;

import com.tramchester.graph.CachedNodeOperations;
import com.tramchester.graph.JourneyState;
import com.tramchester.graph.TransportGraphBuilder;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import static com.tramchester.graph.TransportRelationshipTypes.BOARD;
import static com.tramchester.graph.TransportRelationshipTypes.INTERCHANGE_BOARD;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class StationState extends TraversalState {

    public StationState(TraversalState parent, CachedNodeOperations nodeOperations, Iterable<Relationship> relationships, long destinationNodeId) {
        super(parent, nodeOperations, relationships, destinationNodeId);
    }

    @Override
    public String toString() {
        return "StationState{" +
                "parent=" + parent +
                '}';
    }

    @Override
    public TraversalState nextState(Path path, TransportGraphBuilder.Labels nodeLabel, Node node, JourneyState journeyState) {
        journeyState.updateJourneyClock(getTotalCost(path));

        if (node.getId()==destinationNodeId) {
            return new DestinationState(this, nodeOperations, destinationNodeId);
        }
        if (nodeLabel == TransportGraphBuilder.Labels.PLATFORM) {
            return new PlatformState(this, nodeOperations,
                    node.getRelationships(OUTGOING, INTERCHANGE_BOARD, BOARD), node.getId(), destinationNodeId);
        }
        throw new RuntimeException("Unexpected node type: "+nodeLabel);

    }
}

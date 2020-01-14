package com.tramchester.graph.states;

import com.tramchester.graph.JourneyState;
import com.tramchester.graph.TransportGraphBuilder;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import static com.tramchester.graph.TransportRelationshipTypes.BOARD;
import static com.tramchester.graph.TransportRelationshipTypes.INTERCHANGE_BOARD;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class StationState extends TraversalState {

    public StationState(TraversalState parent, Iterable<Relationship> relationships, int cost) {
        super(parent, relationships, cost);
    }

    @Override
    public String toString() {
        return "StationState{" +
                "cost=" + super.getCurrentCost() +
                ", parent=" + parent +
                '}';
    }

    @Override
    public TraversalState nextState(Path path, TransportGraphBuilder.Labels nodeLabel, Node node, JourneyState journeyState, int cost) {
        if (node.getId()==destinationNodeId) {
            // TODO Cost of platform depart?
            return new DestinationState(this, cost);
        }
        if (nodeLabel == TransportGraphBuilder.Labels.PLATFORM) {
            return new PlatformState(this,
                    node.getRelationships(OUTGOING, INTERCHANGE_BOARD, BOARD), node.getId(), cost);
        }
        throw new RuntimeException("Unexpected node type: "+nodeLabel);

    }
}

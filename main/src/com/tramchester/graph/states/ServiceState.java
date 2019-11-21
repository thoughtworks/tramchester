package com.tramchester.graph.states;

import com.tramchester.graph.CachedNodeOperations;
import com.tramchester.graph.JourneyState;
import com.tramchester.graph.TransportGraphBuilder;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import static com.tramchester.graph.TransportRelationshipTypes.TO_MINUTE;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class ServiceState extends TraversalState {

    public ServiceState(TraversalState parent, CachedNodeOperations nodeOperations, Iterable<Relationship> relationships,
                        long destinationNodeId) {
        super(parent, nodeOperations, relationships, destinationNodeId);
    }

    @Override
    public String toString() {
        return "ServiceState{" +
                "parent=" + parent +
                '}';
    }

    @Override
    public TraversalState nextState(Path path, TransportGraphBuilder.Labels nodeLabel, Node node, JourneyState journeyState) {
        if (nodeLabel == TransportGraphBuilder.Labels.HOUR) {
            Iterable<Relationship> relationships = timeOrdered(node.getRelationships(OUTGOING, TO_MINUTE));
            return new HourState(this, nodeOperations, relationships, destinationNodeId);
        }
        throw new RuntimeException("Unexpected node type: "+nodeLabel);
    }

}

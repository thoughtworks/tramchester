package com.tramchester.graph.states;

import com.tramchester.graph.JourneyState;
import com.tramchester.graph.TransportGraphBuilder;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import static com.tramchester.graph.TransportRelationshipTypes.ENTER_PLATFORM;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class WalkingState extends TraversalState {

    public WalkingState(TraversalState parent, Iterable<Relationship> relationships) {
        super(parent, relationships);
    }

    @Override
    public String toString() {
        return "WalkingState{" +
                "parent=" + parent +
                '}';
    }

    @Override
    public TraversalState nextState(Path path, TransportGraphBuilder.Labels nodeLabel, Node node, JourneyState journeyState) {
        if (!TransportGraphBuilder.Labels.STATION.equals(nodeLabel)) {
            throw new RuntimeException("Unexpected node type: " + nodeLabel);
        }
        return new StationState(this, node.getRelationships(OUTGOING, ENTER_PLATFORM));
    }
}

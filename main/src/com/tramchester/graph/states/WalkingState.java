package com.tramchester.graph.states;

import com.tramchester.graph.JourneyState;
import com.tramchester.graph.TransportGraphBuilder;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import static com.tramchester.graph.TransportRelationshipTypes.ENTER_PLATFORM;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class WalkingState extends TraversalState {
    public WalkingState(TraversalState parent, Iterable<Relationship> relationships, int cost) {
        super(parent, relationships, cost);
    }

    @Override
    public String toString() {
        return "WalkingState{" +
                "cost=" + super.getCurrentCost() +
                ", parent=" + parent +
                '}';
    }

    @Override
    public TraversalState nextState(Path path, TransportGraphBuilder.Labels nodeLabel, Node node, JourneyState journeyState, int cost) {
        if (TransportGraphBuilder.Labels.TRAM_STATION==nodeLabel)   {
            // could be we've walked to our destination
            if (node.getId()==destinationNodeId) {
                return new DestinationState(this, cost);
            }
            return new TramStationState(this, node.getRelationships(OUTGOING, ENTER_PLATFORM), cost, node.getId());
        }
        if (TransportGraphBuilder.Labels.BUS_STATION==nodeLabel) {
            if (node.getId()==destinationNodeId) {
                return new DestinationState(this, cost);
            }
            return new BusStationState(this, node.getRelationships(OUTGOING, ENTER_PLATFORM), cost, node.getId());
        }

        throw new RuntimeException("Unexpected node type: " + nodeLabel);
    }
}

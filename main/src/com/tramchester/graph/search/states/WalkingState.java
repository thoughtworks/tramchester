package com.tramchester.graph.search.states;

import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.search.JourneyState;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class WalkingState extends TraversalState {

    public static class Builder {

        public TraversalState fromBusStation(BusStationState busStationState, Node node, int cost) {
            return new WalkingState(busStationState, node.getRelationships(OUTGOING), cost);
        }

        public TraversalState fromStart(NotStartedState notStartedState, Node firstNode, int cost) {
            return new WalkingState(notStartedState, firstNode.getRelationships(OUTGOING, WALKS_TO), cost);
        }

        public TraversalState fromTramStation(TramStationState tramStationState, Node node, int cost) {
            return new WalkingState(tramStationState, node.getRelationships(OUTGOING), cost);
        }
    }

    private final BusStationState.Builder busStationStateBuilder;
    private final TramStationState.Builder tramStationStateBuilder;

    private WalkingState(TraversalState parent, Iterable<Relationship> relationships, int cost) {
        super(parent, relationships, cost);
        busStationStateBuilder = new BusStationState.Builder();
        tramStationStateBuilder = new TramStationState.Builder();
    }

    @Override
    public String toString() {
        return "WalkingState{" +
                "cost=" + super.getCurrentCost() +
                ", parent=" + parent +
                '}';
    }

    @Override
    public TraversalState createNextState(Path path, GraphBuilder.Labels nodeLabel, Node node, JourneyState journeyState, int cost) {
        // could be we've walked to our destination
        if (node.getId()==destinationNodeId) {
            return new DestinationState(this, cost);
        }

        if (GraphBuilder.Labels.TRAM_STATION==nodeLabel)   {
            return tramStationStateBuilder.fromWalking(this, node, cost);
        }
        if (GraphBuilder.Labels.BUS_STATION==nodeLabel) {
            return busStationStateBuilder.fromWalking(this, node, cost);
        }

        throw new RuntimeException("Unexpected node type: " + nodeLabel);
    }
}

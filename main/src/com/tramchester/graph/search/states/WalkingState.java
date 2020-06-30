package com.tramchester.graph.search.states;

import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.search.JourneyState;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class WalkingState extends TraversalState {

    private WalkingState(TraversalState parent, Iterable<Relationship> relationships, int cost) {
        super(parent, relationships, cost);
    }

    public static TraversalState fromBusStation(BusStationState busStationState, Node node, int cost) {
        return new WalkingState(busStationState, node.getRelationships(OUTGOING), cost);
    }

    public static TraversalState fromStart(NotStartedState notStartedState, Node firstNode, int cost) {
        return new WalkingState(notStartedState, firstNode.getRelationships(OUTGOING, WALKS_TO), cost);
    }

    public static TraversalState fromTramStation(TramStationState tramStationState, Node node, int cost) {
        return new WalkingState(tramStationState, node.getRelationships(OUTGOING), cost);
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
            return TramStationState.fromWalking(this, node, cost);
        }
        if (GraphBuilder.Labels.BUS_STATION==nodeLabel) {
            return BusStationState.fromWalking(this, node, cost);
        }

        throw new RuntimeException("Unexpected node type: " + nodeLabel);
    }
}

package com.tramchester.graph.search.states;

import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.search.JourneyState;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.WALKS_TO;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class WalkingState extends TraversalState {

    public static class Builder {

        public TraversalState fromStart(NotStartedState notStartedState, Node firstNode, int cost) {
            return new WalkingState(notStartedState, firstNode.getRelationships(OUTGOING, WALKS_TO), cost);
        }

        public TraversalState fromNoPlatformStation(NoPlatformStationState noPlatformStation, Node node, int cost) {
            return new WalkingState(noPlatformStation,
                    filterExcludingEndNode(node.getRelationships(OUTGOING), noPlatformStation), cost);
        }

        public TraversalState fromTramStation(TramStationState tramStationState, Node node, int cost) {
            return new WalkingState(tramStationState,
                    filterExcludingEndNode(node.getRelationships(OUTGOING), tramStationState), cost);
        }
    }

    private WalkingState(TraversalState parent, Stream<Relationship> relationships, int cost) {
        super(parent, relationships, cost);
    }

    private WalkingState(TraversalState parent, Iterable<Relationship> relationships, int cost) {
        super(parent, relationships, cost);
    }

    @Override
    public String toString() {
        return "WalkingState{} " + super.toString();
    }

    @Override
    public TraversalState createNextState(GraphBuilder.Labels nodeLabel, Node node, JourneyState journeyState, int cost) {
        // could be we've walked to our destination
        if (destinationNodeIds.contains(node.getId())) {
            return builders.destination.from(this, cost);
        }

        switch (nodeLabel) {
            case TRAM_STATION -> {
                journeyState.walkingConnection();
                return builders.tramStation.fromWalking(this, node, cost);
            }
            case BUS_STATION, TRAIN_STATION -> {
                journeyState.walkingConnection();
                return builders.towardsNeighbour(this, NoPlatformStationState.class).
                        from(this, node, cost);
            }
            case GROUPED -> {
                journeyState.walkingConnection();
                return builders.groupedStation.fromWalk(this, node, cost);
            }
            default -> throw new RuntimeException("Unexpected node type: " + nodeLabel + " at " + this);
        }

    }
}

package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.search.JourneyState;
import com.tramchester.graph.search.stateMachine.*;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.GROUPED_TO_CHILD;

public class GroupedStationState extends TraversalState {

    public static class Builder {

        // TODO map of accept states to outbound relationships

        public TraversalState fromChildStation(TramStationState tramStationState, Node node, int cost) {
            return new GroupedStationState(tramStationState,
                    filterExcludingEndNode(node.getRelationships(Direction.OUTGOING, GROUPED_TO_CHILD),tramStationState),
                    cost, node.getId());
        }

        public TraversalState fromChildStation(NoPlatformStationState noPlatformStationState, Node node, int cost) {
            return new GroupedStationState(noPlatformStationState,
                    filterExcludingEndNode(node.getRelationships(Direction.OUTGOING, GROUPED_TO_CHILD), noPlatformStationState),
                    cost, node.getId());
        }

        public TraversalState fromWalk(WalkingState walkingState, Node node, int cost) {
            return new GroupedStationState(walkingState, node.getRelationships(Direction.OUTGOING, GROUPED_TO_CHILD),
                    cost, node.getId());
        }

        public TraversalState fromStart(NotStartedState notStartedState, Node node, int cost) {
            return new GroupedStationState(notStartedState, node.getRelationships(Direction.OUTGOING, GROUPED_TO_CHILD),
                    cost, node.getId());
        }
    }

    private final long stationNodeId;

    private GroupedStationState(TraversalState parent, Stream<Relationship> relationships, int cost, long stationNodeId) {
        super(parent, relationships, cost);
        this.stationNodeId = stationNodeId;
    }

    private GroupedStationState(TraversalState parent, Iterable<Relationship> relationships, int cost, long stationNodeId) {
        super(parent, relationships, cost);
        this.stationNodeId = stationNodeId;
    }

    @Override
    public String toString() {
        return "GroupedStationState{" +
                "stationNodeId=" + stationNodeId +
                "} " + super.toString();
    }

    @Override
    protected TraversalState createNextState(GraphBuilder.Labels nodeLabel, Node next, JourneyState journeyState, int cost) {
        long nodeId = next.getId();
        if (traversalOps.isDestination(nodeId)) {
            // TODO Cost of bus depart?
            return builders.destination.from(this, cost);
        }

        switch (nodeLabel) {
            case BUS_STATION:
            case TRAIN_STATION:
                return builders.towardsNeighbour(this, NoPlatformStationState.class).fromGrouped(this, next, cost);
            case TRAM_STATION:
                return builders.towardsNeighbour(this, TramStationState.class).fromGrouped(this, next, cost);
            default:
                String message = "Unexpected node type: " + nodeLabel + " at " + this + " for " + journeyState;
                throw new UnexpectedNodeTypeException(next, message);
        }
    }
}

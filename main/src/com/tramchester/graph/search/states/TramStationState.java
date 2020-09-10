package com.tramchester.graph.search.states;

import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.search.JourneyState;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class TramStationState extends TraversalState {

    public static class Builder {
        public TraversalState fromWalking(WalkingState walkingState, Node node, int cost) {
            return new TramStationState(walkingState, node.getRelationships(OUTGOING, ENTER_PLATFORM), cost, node.getId());
        }

        public TraversalState fromPlatform(PlatformState platformState, Node node, int cost) {
            return new TramStationState(platformState,
                    filterExcludingEndNode(
                            node.getRelationships(OUTGOING, ENTER_PLATFORM, WALKS_FROM, BUS_NEIGHBOUR, TRAIN_NEIGHBOUR), platformState),
                    cost, node.getId());
        }

        public TraversalState fromStart(NotStartedState notStartedState, Node node, int cost) {
            return new TramStationState(notStartedState,
                    node.getRelationships(OUTGOING, ENTER_PLATFORM, WALKS_FROM, BUS_NEIGHBOUR, TRAIN_NEIGHBOUR),
                    cost, node.getId());
        }

        public TraversalState fromNeighbour(NoPlatformStationState noPlatformStation, Node node, int cost) {
            return new TramStationState(noPlatformStation, node.getRelationships(OUTGOING, ENTER_PLATFORM), cost, node.getId());
        }

    }

    private final long stationNodeId;

    private TramStationState(TraversalState parent, Iterable<Relationship> relationships, int cost, long stationNodeId) {
        super(parent, relationships, cost);
        this.stationNodeId = stationNodeId;
    }

    @Override
    public String toString() {
        return "TramStationState{" +
                "stationNodeId=" + stationNodeId +
                "} " + super.toString();
    }

    @Override
    public TraversalState createNextState(GraphBuilder.Labels nodeLabel, Node node,
                                          JourneyState journeyState, int cost) {
        long nodeId = node.getId();
        if (destinationNodeIds.contains(nodeId)) {
            // TODO Cost of platform depart?
            return builders.destination.from(this, cost);
        }

        switch (nodeLabel) {
            case PLATFORM:
                return builders.platform.from(this, node, cost);
            case QUERY_NODE:
//            case QUERY_NODE_MID:
                journeyState.connection();
                return builders.walking.fromTramStation(this, node, cost);
            case BUS_STATION:
            case TRAIN_STATION:
                return builders.noPlatformStation.fromNeighbour(this, node, cost, nodeLabel);
            default:
                throw new RuntimeException("Unexpected node type: "+nodeLabel+ " at " + toString());

        }
    }
}

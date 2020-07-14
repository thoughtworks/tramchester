package com.tramchester.graph.search.states;

import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.search.JourneyState;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
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
                    filterExcludingEndNode(node.getRelationships(OUTGOING, ENTER_PLATFORM, WALKS_FROM, BUS_NEIGHBOUR), platformState),
                    cost, node.getId());
        }

        public TraversalState fromStart(NotStartedState notStartedState, Node node, int cost) {
            return new TramStationState(notStartedState,
                    node.getRelationships(OUTGOING, ENTER_PLATFORM, WALKS_FROM, BUS_NEIGHBOUR),
                    cost, node.getId());
        }

        public TraversalState fromNeighbour(BusStationState busStationState, Node node, int cost) {
            return new TramStationState(busStationState, node.getRelationships(OUTGOING, ENTER_PLATFORM), cost, node.getId());
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
    public TraversalState createNextState(Path path, GraphBuilder.Labels nodeLabel, Node node,
                                          JourneyState journeyState, int cost) {
        long nodeId = node.getId();
        if (destinationNodeIds.contains(nodeId)) {
            // TODO Cost of platform depart?
            return builders.destination.from(this, cost);
        }

        if (nodeLabel == GraphBuilder.Labels.PLATFORM) {
            return builders.platform.from(this, node, cost);
        }
        if (nodeLabel == GraphBuilder.Labels.QUERY_NODE || nodeLabel == GraphBuilder.Labels.QUERY_NODE_MID) {
            return builders.walking.fromTramStation(this, node, cost);
        }
        if (nodeLabel == GraphBuilder.Labels.BUS_STATION) {
            return builders.busStation.fromNeighbour(this, node, cost);
        }

        throw new RuntimeException("Unexpected node type: "+nodeLabel+ " at " + toString());

    }
}

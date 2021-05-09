package com.tramchester.graph.search.states;

import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.search.JourneyState;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class TramStationState extends TraversalState implements NodeId {

    public static class Builder {
        public TraversalState fromWalking(WalkingState walkingState, Node node, int cost) {
            return new TramStationState(walkingState, node.getRelationships(OUTGOING, ENTER_PLATFORM, GROUPED_TO_PARENT, NEIGHBOUR), cost, node.getId());
        }

        public TraversalState fromPlatform(PlatformState platformState, Node node, int cost) {
            return new TramStationState(platformState,
                    filterExcludingEndNode(
                            node.getRelationships(OUTGOING, ENTER_PLATFORM, WALKS_FROM, NEIGHBOUR, GROUPED_TO_PARENT), platformState),
                    cost, node.getId());
        }

        public TraversalState fromStart(NotStartedState notStartedState, Node node, int cost) {
            return new TramStationState(notStartedState,
                    node.getRelationships(OUTGOING, ENTER_PLATFORM, WALKS_FROM, NEIGHBOUR, GROUPED_TO_PARENT),
                    cost, node.getId());
        }

        public TraversalState fromNeighbour(NoPlatformStationState noPlatformStation, Node node, int cost) {
            return new TramStationState(noPlatformStation,
                    node.getRelationships(OUTGOING, ENTER_PLATFORM, GROUPED_TO_PARENT), cost, node.getId());
        }

        public TraversalState fromNeighbour(TramStationState tramStationState, Node node, int cost) {
            return new TramStationState(tramStationState,
                    node.getRelationships(OUTGOING, ENTER_PLATFORM, GROUPED_TO_PARENT), cost, node.getId());
        }

        public TraversalState fromGrouped(GroupedStationState groupedStationState, Node node, int cost) {
            return new TramStationState(groupedStationState,
                    node.getRelationships(OUTGOING, ENTER_PLATFORM, NEIGHBOUR),
                    cost,  node.getId());
        }
    }

    private final long stationNodeId;

    private TramStationState(TraversalState parent, Stream<Relationship> relationships, int cost, long stationNodeId) {
        super(parent, relationships, cost);
        this.stationNodeId = stationNodeId;
    }

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
    public long nodeId() {
        return stationNodeId;
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
                journeyState.walkingConnection();
                return builders.walking.fromTramStation(this, node, cost);
            case BUS_STATION:
            case TRAIN_STATION:
                return builders.towardsNeighbourFromTramStation(this, NoPlatformStationState.class).
                        fromNeighbour(this, node, cost);
            case TRAM_STATION:
                return builders.tramStation.fromNeighbour(this, node, cost);
            case GROUPED:
                return builders.groupedStation.fromChildStation(this, node, cost); // grouped are same transport mode
            default:
                String message = "Unexpected node type: " + nodeLabel + " at " + this + " for " + journeyState;
                throw new UnexpectedNodeTypeException(node, message);
        }
    }
}

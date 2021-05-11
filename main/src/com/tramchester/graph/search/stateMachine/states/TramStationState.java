package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.search.JourneyState;
import com.tramchester.graph.search.stateMachine.*;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class TramStationState extends StationState {

    public static class Builder implements TowardsStationState<TramStationState> {

        @Override
        public void register(RegistersFromState registers) {
            registers.add(WalkingState.class, this);
            registers.add(PlatformState.class, this);
            registers.add(NotStartedState.class, this);
            registers.add(NoPlatformStationState.class, this);
            registers.add(TramStationState.class, this);
            registers.add(GroupedStationState.class, this);
        }

        @Override
        public Class<TramStationState> getDestination() {
            return TramStationState.class;
        }

        public TramStationState fromWalking(WalkingState walkingState, Node node, int cost) {
            return new TramStationState(walkingState, node.getRelationships(OUTGOING, ENTER_PLATFORM, GROUPED_TO_PARENT, NEIGHBOUR), cost, node.getId());
        }

        public TramStationState fromPlatform(PlatformState platformState, Node node, int cost) {
            return new TramStationState(platformState,
                    filterExcludingEndNode(
                            node.getRelationships(OUTGOING, ENTER_PLATFORM, WALKS_FROM, NEIGHBOUR, GROUPED_TO_PARENT), platformState),
                    cost, node.getId());
        }

        public TramStationState fromStart(NotStartedState notStartedState, Node node, int cost) {
            return new TramStationState(notStartedState,
                    node.getRelationships(OUTGOING, ENTER_PLATFORM, WALKS_FROM, NEIGHBOUR, GROUPED_TO_PARENT),
                    cost, node.getId());
        }

        @Override
        public TramStationState fromNeighbour(StationState stationState, Node next, int cost) {
                return new TramStationState(stationState,
                        next.getRelationships(OUTGOING, ENTER_PLATFORM, GROUPED_TO_PARENT), cost, next.getId());
        }

        public TramStationState fromGrouped(GroupedStationState groupedStationState, Node node, int cost) {
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
        if (traversalOps.isDestination(nodeId)) {
            // TODO Cost of platform depart?
            return builders.towardsDest(this).from(this, cost);
        }

        String message = "Unexpected node type: " + nodeLabel + " at " + this + " for " + journeyState;
        throw new UnexpectedNodeTypeException(node, message);

//        switch (nodeLabel) {
//            case PLATFORM:
//                return builders.towardsPlatform(this).from(this, node, cost);
//            case QUERY_NODE:
//                journeyState.walkingConnection();
//                return builders.towardsWalk(this).fromStation(this, node, cost);
//            case BUS_STATION:
//            case TRAIN_STATION:
//                return builders.towardsNeighbour(this, NoPlatformStationState.class).
//                        fromNeighbour(this, node, cost);
//            case TRAM_STATION:
//                return builders.towardsNeighbour(this, TramStationState.class).fromNeighbour(this, node, cost);
//            case GROUPED:
//                return builders.towardsGroup(this).fromChildStation(this, node, cost); // grouped are same transport mode
//            default:
//                String message = "Unexpected node type: " + nodeLabel + " at " + this + " for " + journeyState;
//                throw new UnexpectedNodeTypeException(node, message);
//        }

    }

    @Override
    protected TraversalState toWalk(WalkingState.Builder towardsWalk, Node node, int cost, JourneyState journeyState) {
        journeyState.walkingConnection();
        return towardsWalk.fromStation(this, node, cost);
    }

    @Override
    protected TraversalState toNoPlatformStation(NoPlatformStationState.Builder toStation, Node node, int cost, JourneyState journeyState) {
        return toStation.fromNeighbour(this, node, cost);
    }

    @Override
    protected TramStationState toTramStation(Builder towardsStation, Node node, int cost, JourneyState journeyState) {
        return towardsStation.fromNeighbour(this, node, cost);
    }

    @Override
    protected TraversalState toGrouped(GroupedStationState.Builder towardsGroup, Node node, int cost, JourneyState journeyState) {
        return towardsGroup.fromChildStation(this, node, cost);
    }

    @Override
    protected TraversalState toPlatform(PlatformState.Builder towardsPlatform, Node node, int cost, JourneyState journeyState) {
        return towardsPlatform.from(this, node, cost);
    }
}

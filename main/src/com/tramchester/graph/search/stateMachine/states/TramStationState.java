package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.TowardsStation;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.Direction.OUTGOING;

// todo rename PlatformStationState
public class TramStationState extends StationState {

    public static class Builder implements TowardsStation<TramStationState> {

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

        public TramStationState fromWalking(WalkingState walkingState, Node stationNode, int cost) {
            final Iterable<Relationship> relationships = stationNode.getRelationships(OUTGOING, ENTER_PLATFORM, GROUPED_TO_PARENT,
                    NEIGHBOUR);
            return new TramStationState(walkingState, relationships, cost, stationNode);
        }

        public TramStationState fromPlatform(PlatformState platformState, Node stationNode, int cost) {
            final Iterable<Relationship> relationships = stationNode.getRelationships(OUTGOING, WALKS_FROM, ENTER_PLATFORM,
                    NEIGHBOUR, GROUPED_TO_PARENT);
            return new TramStationState(platformState, filterExcludingEndNode(relationships, platformState), cost, stationNode);
        }

        public TramStationState fromStart(NotStartedState notStartedState, Node stationNode, int cost) {
            final Iterable<Relationship> relationships = stationNode.getRelationships(OUTGOING, WALKS_FROM, ENTER_PLATFORM,
                    NEIGHBOUR, GROUPED_TO_PARENT);
            return new TramStationState(notStartedState, relationships, cost, stationNode);
        }

        @Override
        public TramStationState fromNeighbour(StationState stationState, Node stationNode, int cost) {
            final Iterable<Relationship> relationships = stationNode.getRelationships(OUTGOING, ENTER_PLATFORM, GROUPED_TO_PARENT);
            return new TramStationState(stationState, relationships, cost, stationNode);
        }

        public TramStationState fromGrouped(GroupedStationState groupedStationState, Node stationNode, int cost) {
            final Iterable<Relationship> relationships = stationNode.getRelationships(OUTGOING, ENTER_PLATFORM, NEIGHBOUR);
            return new TramStationState(groupedStationState, relationships, cost, stationNode);
        }

    }

    private final Node stationNode;

    private TramStationState(TraversalState parent, Stream<Relationship> relationships, int cost, Node stationNode) {
        super(parent, relationships, cost);
        this.stationNode = stationNode;
    }

    private TramStationState(TraversalState parent, Iterable<Relationship> relationships, int cost, Node stationNode) {
        super(parent, relationships, cost);
        this.stationNode = stationNode;
    }

    @Override
    public String toString() {
        return "TramStationState{" +
                "stationNodeId=" + stationNode.getId() +
                "} " + super.toString();
    }


    @Override
    public long nodeId() {
        return stationNode.getId();
    }

    @Override
    protected TraversalState toWalk(WalkingState.Builder towardsWalk, Node node, int cost, JourneyStateUpdate journeyState) {
        journeyState.beginWalk(stationNode, false, cost);
        return towardsWalk.fromStation(this, node, cost);
    }

    @Override
    protected TraversalState toNoPlatformStation(NoPlatformStationState.Builder toStation, Node node, int cost, JourneyStateUpdate journeyState) {
        journeyState.toNeighbour(stationNode, node, cost);
        return toStation.fromNeighbour(this, node, cost);
    }

    @Override
    protected TramStationState toTramStation(Builder towardsStation, Node node, int cost, JourneyStateUpdate journeyState) {
        journeyState.toNeighbour(stationNode, node, cost);
        return towardsStation.fromNeighbour(this, node, cost);
    }

    @Override
    protected TraversalState toGrouped(GroupedStationState.Builder towardsGroup, Node node, int cost, JourneyStateUpdate journeyState) {
        return towardsGroup.fromChildStation(this, node, cost);
    }

    @Override
    protected TraversalState toPlatform(PlatformState.Builder towardsPlatform, Node node, int cost, JourneyStateUpdate journeyState) {
        return towardsPlatform.from(this, node, cost);
    }

    @Override
    protected void toDestination(DestinationState.Builder towardsDestination, Node node, int cost, JourneyStateUpdate journeyStateUpdate) {
        towardsDestination.from(this, cost);
    }
}

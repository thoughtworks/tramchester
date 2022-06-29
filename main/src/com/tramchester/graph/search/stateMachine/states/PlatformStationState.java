package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.TowardsStation;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.time.Duration;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class PlatformStationState extends StationState {

    public static class Builder extends StationStateBuilder implements TowardsStation<PlatformStationState>  {

        @Override
        public void register(RegistersFromState registers) {
            registers.add(WalkingState.class, this);
            registers.add(PlatformState.class, this);
            registers.add(NotStartedState.class, this);
            registers.add(NoPlatformStationState.class, this);
            registers.add(PlatformStationState.class, this);
            registers.add(GroupedStationState.class, this);
        }

        @Override
        public Class<PlatformStationState> getDestination() {
            return PlatformStationState.class;
        }

        public PlatformStationState fromWalking(WalkingState walkingState, Node stationNode, Duration cost, JourneyStateUpdate journeyState) {
            final Iterable<Relationship> relationships = stationNode.getRelationships(OUTGOING, ENTER_PLATFORM, GROUPED_TO_PARENT,
                    NEIGHBOUR);
            return new PlatformStationState(walkingState, relationships, cost, stationNode, journeyState);
        }

        public PlatformStationState fromPlatform(PlatformState platformState, Node stationNode, Duration cost,
                                                 JourneyStateUpdate journeyState, boolean onDiversion) {
            final Iterable<Relationship> initial = stationNode.getRelationships(OUTGOING, WALKS_FROM_STATION, ENTER_PLATFORM,
                    NEIGHBOUR, GROUPED_TO_PARENT);
            Stream<Relationship> relationships = addValidDiversions(stationNode, initial, platformState, onDiversion);
            return new PlatformStationState(platformState, filterExcludingEndNode(relationships, platformState), cost,
                    stationNode, journeyState);
        }

        public PlatformStationState fromStart(NotStartedState notStartedState, Node stationNode, Duration cost, JourneyStateUpdate journeyState, boolean alreadyOnDiversion, boolean onDiversion) {
            final Iterable<Relationship> initial = stationNode.getRelationships(OUTGOING, WALKS_FROM_STATION, ENTER_PLATFORM,
                    NEIGHBOUR, GROUPED_TO_PARENT);
            Stream<Relationship> relationships = addValidDiversions(stationNode, initial, notStartedState, onDiversion);
            return new PlatformStationState(notStartedState, relationships, cost, stationNode, journeyState);
        }

        @Override
        public PlatformStationState fromNeighbour(StationState stationState, Node stationNode, Duration cost, JourneyStateUpdate journeyState, boolean onDiversion) {
            final Iterable<Relationship> initial = stationNode.getRelationships(OUTGOING, ENTER_PLATFORM, GROUPED_TO_PARENT);
            Stream<Relationship> relationships = addValidDiversions(stationNode, initial, stationState, onDiversion);
            return new PlatformStationState(stationState, relationships, cost, stationNode, journeyState);
        }

        public PlatformStationState fromGrouped(GroupedStationState groupedStationState, Node stationNode, Duration cost,
                                                JourneyStateUpdate journeyState) {
            final Iterable<Relationship> relationships = stationNode.getRelationships(OUTGOING, ENTER_PLATFORM, NEIGHBOUR);
            return new PlatformStationState(groupedStationState, relationships, cost, stationNode, journeyState);
        }

    }

    private PlatformStationState(TraversalState parent, Stream<Relationship> relationships, Duration cost, Node stationNode,
                                 JourneyStateUpdate journeyState) {
        super(parent, relationships, cost, stationNode, journeyState);
    }

    private PlatformStationState(TraversalState parent, Iterable<Relationship> relationships, Duration cost, Node stationNode,
                                 JourneyStateUpdate journeyState) {
        super(parent, relationships, cost, stationNode, journeyState);
    }

    @Override
    public String toString() {
        return "PlatformStationState{" +
                "stationNodeId=" + stationNode.getId() +
                "} " + super.toString();
    }


    @Override
    public long nodeId() {
        return stationNode.getId();
    }

    @Override
    protected TraversalState toWalk(WalkingState.Builder towardsWalk, Node node, Duration cost, JourneyStateUpdate journeyState) {
        journeyState.beginWalk(stationNode, false, cost);
        return towardsWalk.fromStation(this, node, cost);
    }

    @Override
    protected TraversalState toNoPlatformStation(NoPlatformStationState.Builder toStation, Node node, Duration cost, JourneyStateUpdate journeyState, boolean onDiversion) {
        journeyState.toNeighbour(stationNode, node, cost);
        return toStation.fromNeighbour(this, node, cost, journeyState, onDiversion);
    }

    @Override
    protected PlatformStationState toTramStation(Builder towardsStation, Node node, Duration cost, JourneyStateUpdate journeyState, boolean onDiversion) {
        journeyState.toNeighbour(stationNode, node, cost);
        return towardsStation.fromNeighbour(this, node, cost, journeyState, onDiversion);
    }

    @Override
    protected TraversalState toGrouped(GroupedStationState.Builder towardsGroup, Node node, Duration cost, JourneyStateUpdate journeyState) {
        return towardsGroup.fromChildStation(this, node, cost);
    }

    @Override
    protected TraversalState toPlatform(PlatformState.Builder towardsPlatform, Node node, Duration cost, JourneyStateUpdate journeyState) {
        return towardsPlatform.from(this, node, cost);
    }

    @Override
    protected void toDestination(DestinationState.Builder towardsDestination, Node node, Duration cost, JourneyStateUpdate journeyStateUpdate) {
        towardsDestination.from(this, cost);
    }
}

package com.tramchester.graph.search.stateMachine.states;

import com.google.common.collect.Streams;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.TowardsStation;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterable;

import java.time.Duration;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class NoPlatformStationState extends StationState {

    public static class Builder extends StationStateBuilder implements TowardsStation<NoPlatformStationState> {

        @Override
        public void register(RegistersFromState registers) {
            registers.add(TraversalStateType.WalkingState, this);
            registers.add(TraversalStateType.NotStartedState, this);
            registers.add(TraversalStateType.RouteStationStateOnTrip, this);
            registers.add(TraversalStateType.RouteStationStateEndTrip, this);
            registers.add(TraversalStateType.NoPlatformStationState, this);
            registers.add(TraversalStateType.PlatformStationState, this);
            registers.add(TraversalStateType.GroupedStationState, this);
        }

        @Override
        public TraversalStateType getDestination() {
            return TraversalStateType.NoPlatformStationState;
        }

        public NoPlatformStationState fromWalking(WalkingState walkingState, Node node, Duration cost, JourneyStateUpdate journeyState) {
            return new NoPlatformStationState(walkingState,
                    boardRelationshipsPlus(node, GROUPED_TO_PARENT, NEIGHBOUR),
                    cost, node, journeyState, this);
        }

        public NoPlatformStationState fromStart(NotStartedState notStartedState, Node node, Duration cost,
                                                JourneyStateUpdate journeyState, boolean alreadyOnDiversion, boolean onDiversion) {
            final Stream<Relationship> neighbours = getRelationships(node, OUTGOING, NEIGHBOUR);
            final Stream<Relationship> initial = boardRelationshipsPlus(node, WALKS_FROM_STATION, GROUPED_TO_PARENT);
            Stream<Relationship> relationships = addValidDiversions(node, initial, notStartedState, alreadyOnDiversion);

            return new NoPlatformStationState(notStartedState, Stream.concat(neighbours, relationships), cost, node, journeyState, this);
        }

        public TraversalState fromRouteStation(RouteStationStateEndTrip routeStationState, Node node, Duration cost,
                                               JourneyStateUpdate journeyState, boolean alreadyOnDiversion) {
            // end of a trip, may need to go back to this route station to catch new service
            final Stream<Relationship> initial = boardRelationshipsPlus(node, WALKS_FROM_STATION, NEIGHBOUR, GROUPED_TO_PARENT);
            Stream<Relationship> relationships = addValidDiversions(node, initial, routeStationState, alreadyOnDiversion);
            return new NoPlatformStationState(routeStationState, relationships, cost, node, journeyState, this);
        }

        public TraversalState fromRouteStation(RouteStationStateOnTrip onTrip, Node node, Duration cost, JourneyStateUpdate journeyState) {
            // filter so we don't just get straight back on tram if just boarded, or if we are on an existing trip
            final Stream<Relationship> relationships = boardRelationshipsPlus(node, WALKS_FROM_STATION, NEIGHBOUR, GROUPED_TO_PARENT);
            Stream<Relationship> stationRelationships = filterExcludingEndNode(relationships, onTrip);
            return new NoPlatformStationState(onTrip, stationRelationships, cost, node, journeyState, this);
        }

        public NoPlatformStationState fromNeighbour(StationState noPlatformStation, Node node, Duration cost, JourneyStateUpdate journeyState, boolean onDiversion) {
            return new NoPlatformStationState(noPlatformStation,
                    node.getRelationships(OUTGOING, BOARD, INTERCHANGE_BOARD, GROUPED_TO_PARENT),
                    cost, node, journeyState, this);
        }

        public NoPlatformStationState fromGrouped(GroupedStationState groupedStationState, Node node, Duration cost, JourneyStateUpdate journeyState) {
            return new NoPlatformStationState(groupedStationState,
                    node.getRelationships(OUTGOING, BOARD, INTERCHANGE_BOARD, NEIGHBOUR),
                    cost,  node, journeyState, this);
        }

        Stream<Relationship> boardRelationshipsPlus(Node node, TransportRelationshipTypes... others) {
            Stream<Relationship> other = Streams.stream(node.getRelationships(OUTGOING, others));
            Stream<Relationship> board = Streams.stream(node.getRelationships(OUTGOING, BOARD, INTERCHANGE_BOARD));
            // order matters here, i.e. explore walks first
            return Stream.concat(other, board);
        }

    }

    private NoPlatformStationState(TraversalState parent, Stream<Relationship> relationships, Duration cost, Node stationNode,
                                   JourneyStateUpdate journeyState, TowardsStation<?> builder) {
        super(parent, relationships, cost, stationNode, journeyState, builder);
    }

    private NoPlatformStationState(TraversalState parent, ResourceIterable<Relationship> relationships, Duration cost, Node stationNode,
                                   JourneyStateUpdate journeyState, TowardsStation<?> builder) {
        super(parent, relationships, cost, stationNode, journeyState, builder);
    }

    @Override
    protected PlatformStationState toPlatformStation(PlatformStationState.Builder towardsStation, Node next, Duration cost,
                                                     JourneyStateUpdate journeyState, boolean onDiversion) {
        journeyState.toNeighbour(stationNode, next, cost);
        return towardsStation.fromNeighbour(this, next, cost, journeyState, onDiversion);
    }

    @Override
    protected TraversalState toNoPlatformStation(Builder towardsStation, Node next, Duration cost,
                                                 JourneyStateUpdate journeyState, boolean onDiversion) {
        journeyState.toNeighbour(stationNode, next, cost);
        return towardsStation.fromNeighbour(this, next, cost, journeyState, onDiversion);
    }

    @Override
    protected TraversalState toWalk(WalkingState.Builder towardsWalk, Node walkingNode, Duration cost, JourneyStateUpdate journeyState) {
        journeyState.beginWalk(stationNode, false, cost);
        return towardsWalk.fromStation(this, walkingNode, cost);
    }

    @Override
    protected TraversalState toGrouped(GroupedStationState.Builder towardsGroup, Node groupNode, Duration cost, JourneyStateUpdate journeyState) {
        return towardsGroup.fromChildStation(this, groupNode, cost);
    }

    @Override
    protected JustBoardedState toJustBoarded(JustBoardedState.Builder towardsJustBoarded, Node boardNode, Duration cost, JourneyStateUpdate journeyState) {
        boardVehicle(boardNode, journeyState);
        return towardsJustBoarded.fromNoPlatformStation(this, boardNode, cost);
    }

    @Override
    protected void toDestination(DestinationState.Builder towardsDestination, Node destNode, Duration cost, JourneyStateUpdate journeyStateUpdate) {
        towardsDestination.from(this, cost);
    }

    private void boardVehicle(Node node, JourneyStateUpdate journeyState) {
        try {
            TransportMode actualMode = GraphProps.getTransportMode(node);
            journeyState.board(actualMode, node, false);
        } catch (TramchesterException e) {
            throw new RuntimeException("unable to board vehicle", e);
        }
    }

    @Override
    public String toString() {
        return "NoPlatformStationState{" +
                "stationNodeId=" + stationNode.getId() +
                "} " + super.toString();
    }

    @Override
    public long nodeId() {
        return stationNode.getId();
    }

}

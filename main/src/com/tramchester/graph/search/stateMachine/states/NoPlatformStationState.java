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

import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class NoPlatformStationState extends StationState {

    public static class Builder extends StationStateBuilder implements TowardsStation<NoPlatformStationState> {

        @Override
        public void register(RegistersFromState registers) {
            registers.add(WalkingState.class, this);
            registers.add(NotStartedState.class, this);
            registers.add(RouteStationStateOnTrip.class, this);
            registers.add(RouteStationStateEndTrip.class, this);
            registers.add(NoPlatformStationState.class, this);
            registers.add(TramStationState.class, this);
            registers.add(GroupedStationState.class, this);
        }

        @Override
        public Class<NoPlatformStationState> getDestination() {
            return NoPlatformStationState.class;
        }

        public NoPlatformStationState fromWalking(WalkingState walkingState, Node node, int cost) {
            return new NoPlatformStationState(walkingState,
                    boardRelationshipsPlus(node, GROUPED_TO_PARENT, NEIGHBOUR),
                    cost, node);
        }

        public NoPlatformStationState fromStart(NotStartedState notStartedState, Node node, int cost) {
            final Stream<Relationship> initial = boardRelationshipsPlus(node, WALKS_FROM, NEIGHBOUR, GROUPED_TO_PARENT);
            Stream<Relationship> relationships = addValidDiversions(node, initial, notStartedState);
            return new NoPlatformStationState(notStartedState, relationships, cost, node);
        }

        public TraversalState fromRouteStation(RouteStationStateEndTrip routeStationState, Node node, int cost) {
            // end of a trip, may need to go back to this route station to catch new service
            final Stream<Relationship> initial = boardRelationshipsPlus(node, WALKS_FROM, NEIGHBOUR, GROUPED_TO_PARENT);
            Stream<Relationship> relationships = addValidDiversions(node, initial, routeStationState);
            return new NoPlatformStationState(routeStationState, relationships, cost, node);
        }

        public TraversalState fromRouteStation(RouteStationStateOnTrip onTrip, Node node, int cost) {
            // filter so we don't just get straight back on tram if just boarded, or if we are on an existing trip
            final Stream<Relationship> relationships = boardRelationshipsPlus(node, WALKS_FROM, NEIGHBOUR, GROUPED_TO_PARENT);
            Stream<Relationship> stationRelationships = filterExcludingEndNode(relationships, onTrip);
            return new NoPlatformStationState(onTrip, stationRelationships, cost, node);
        }

        public NoPlatformStationState fromNeighbour(StationState noPlatformStation, Node node, int cost) {
            return new NoPlatformStationState(noPlatformStation,
                    node.getRelationships(OUTGOING, BOARD, INTERCHANGE_BOARD, GROUPED_TO_PARENT),
                    cost, node);
        }

        public NoPlatformStationState fromGrouped(GroupedStationState groupedStationState, Node node, int cost) {
            return new NoPlatformStationState(groupedStationState,
                    node.getRelationships(OUTGOING, BOARD, INTERCHANGE_BOARD, NEIGHBOUR),
                    cost,  node);
        }

        Stream<Relationship> boardRelationshipsPlus(Node node, TransportRelationshipTypes... others) {
            Stream<Relationship> other = Streams.stream(node.getRelationships(OUTGOING, others));
            Stream<Relationship> board = Streams.stream(node.getRelationships(OUTGOING, BOARD, INTERCHANGE_BOARD));
            // order matters here, i.e. explore walks first
            return Stream.concat(other, board);
        }

    }

    private final Node stationNode;

    private NoPlatformStationState(TraversalState parent, Stream<Relationship> relationships, int cost, Node stationNode) {
        super(parent, relationships, cost);
        this.stationNode = stationNode;
    }

    private NoPlatformStationState(TraversalState parent, Iterable<Relationship> relationships, int cost, Node stationNode) {
        super(parent, relationships, cost);
        this.stationNode = stationNode;
    }

    @Override
    protected TramStationState toTramStation(TramStationState.Builder towardsStation, Node next, int cost, JourneyStateUpdate journeyState) {
        journeyState.toNeighbour(stationNode, next, cost);
        return towardsStation.fromNeighbour(this, next, cost);
    }

    @Override
    protected TraversalState toNoPlatformStation(Builder towardsStation, Node next, int cost, JourneyStateUpdate journeyState) {
        journeyState.toNeighbour(stationNode, next, cost);
        return towardsStation.fromNeighbour(this, next, cost);
    }

    @Override
    protected TraversalState toWalk(WalkingState.Builder towardsWalk, Node walkingNode, int cost, JourneyStateUpdate journeyState) {
        journeyState.beginWalk(stationNode, false, cost);
        return towardsWalk.fromStation(this, walkingNode, cost);
    }

    @Override
    protected TraversalState toGrouped(GroupedStationState.Builder towardsGroup, Node groupNode, int cost, JourneyStateUpdate journeyState) {
        return towardsGroup.fromChildStation(this, groupNode, cost);
    }

    @Override
    protected JustBoardedState toJustBoarded(JustBoardedState.Builder towardsJustBoarded, Node boardNode, int cost, JourneyStateUpdate journeyState) {
        boardVehicle(boardNode, journeyState);
        return towardsJustBoarded.fromNoPlatformStation(this, boardNode, cost);
    }

    @Override
    protected void toDestination(DestinationState.Builder towardsDestination, Node destNode, int cost, JourneyStateUpdate journeyStateUpdate) {
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

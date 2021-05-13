package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.reference.TransportMode;
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

    public static class Builder implements TowardsStation<NoPlatformStationState> {

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
                    node.getRelationships(OUTGOING, BOARD, INTERCHANGE_BOARD, GROUPED_TO_PARENT, NEIGHBOUR),
                    cost, node.getId());
        }

        public NoPlatformStationState fromStart(NotStartedState notStartedState, Node node, int cost) {
            return new NoPlatformStationState(notStartedState, getAll(node), cost, node.getId());
        }

        public TraversalState fromRouteStation(RouteStationStateEndTrip routeStationState, Node node, int cost) {
            // end of a trip, may need to go back to this route station to catch new service
            return new NoPlatformStationState(routeStationState, getAll(node), cost, node.getId());
        }

        public TraversalState fromRouteStation(RouteStationStateOnTrip onTrip, Node node, int cost) {
            // filter so we don't just get straight back on tram if just boarded, or if we are on an existing trip
            Stream<Relationship> stationRelationships = filterExcludingEndNode(getAll(node), onTrip);
            return new NoPlatformStationState(onTrip, stationRelationships, cost, node.getId());
        }

        public NoPlatformStationState fromNeighbour(StationState noPlatformStation, Node node, int cost) {
            return new NoPlatformStationState(noPlatformStation,
                    node.getRelationships(OUTGOING, BOARD, INTERCHANGE_BOARD, GROUPED_TO_PARENT),
                    cost, node.getId());
        }

        public NoPlatformStationState fromGrouped(GroupedStationState groupedStationState, Node node, int cost) {
            return new NoPlatformStationState(groupedStationState,
                    node.getRelationships(OUTGOING, BOARD, INTERCHANGE_BOARD, NEIGHBOUR),
                    cost,  node.getId());
        }

        private Iterable<Relationship> getAll(Node node) {
            return node.getRelationships(OUTGOING, INTERCHANGE_BOARD, BOARD, WALKS_FROM, NEIGHBOUR, GROUPED_TO_PARENT);
        }

    }

    private final long stationNodeId;

    private NoPlatformStationState(TraversalState parent, Stream<Relationship> relationships, int cost, long stationNodeId) {
        super(parent, relationships, cost);
        this.stationNodeId = stationNodeId;
    }

    private NoPlatformStationState(TraversalState parent, Iterable<Relationship> relationships, int cost, long stationNodeId) {
        super(parent, relationships, cost);
        this.stationNodeId = stationNodeId;
    }

    @Override
    protected TramStationState toTramStation(TramStationState.Builder towardsStation, Node node, int cost, JourneyStateUpdate journeyState) {
        return towardsStation.fromNeighbour(this, node, cost);
    }

    @Override
    protected TraversalState toNoPlatformStation(Builder towardsStation, Node node, int cost, JourneyStateUpdate journeyState) {
        return towardsStation.fromNeighbour(this, node, cost);
    }

    @Override
    protected TraversalState toWalk(WalkingState.Builder towardsWalk, Node node, int cost, JourneyStateUpdate journeyState) {
        journeyState.beginWalk(node, false);
        return towardsWalk.fromStation(this, node, cost);
    }

    @Override
    protected TraversalState toGrouped(GroupedStationState.Builder towardsGroup, Node node, int cost, JourneyStateUpdate journeyState) {
        return towardsGroup.fromChildStation(this, node, cost);
    }

    @Override
    protected JustBoardedState toJustBoarded(JustBoardedState.Builder towardsJustBoarded, Node node, int cost, JourneyStateUpdate journeyState) {
        boardVehicle(node, journeyState);
        return towardsJustBoarded.fromNoPlatformStation(this, node, cost);
    }

    @Override
    protected DestinationState toDestination(DestinationState.Builder towardsDestination, Node node, int cost, JourneyStateUpdate journeyStateUpdate) {
        return towardsDestination.from(this, cost);
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
                "stationNodeId=" + stationNodeId +
                "} " + super.toString();
    }

    @Override
    public long nodeId() {
        return stationNodeId;
    }

}

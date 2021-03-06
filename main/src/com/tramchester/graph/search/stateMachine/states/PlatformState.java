package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.NodeId;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.Towards;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.List;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class PlatformState extends TraversalState implements NodeId {

    public static class Builder implements Towards<PlatformState> {

        @Override
        public void register(RegistersFromState registers) {
            registers.add(TramStationState.class, this);
            registers.add(RouteStationStateOnTrip.class, this);
            registers.add(RouteStationStateEndTrip.class, this);
        }

        @Override
        public Class<PlatformState> getDestination() {
            return PlatformState.class;
        }

        public PlatformState from(TramStationState tramStationState, Node node, int cost) {
            // inc. board here since might be starting journey
            return new PlatformState(tramStationState,
                    node.getRelationships(OUTGOING, INTERCHANGE_BOARD, BOARD), node, cost);
        }

        public TraversalState fromRouteStationOnTrip(RouteStationStateOnTrip routeStationStateOnTrip, Node node, int cost) {

            // towards final destination, just follow this one
            List<Relationship> towardsDest = routeStationStateOnTrip.traversalOps.
                    getTowardsDestination(node.getRelationships(OUTGOING, LEAVE_PLATFORM));
            if (!towardsDest.isEmpty()) {
                return new PlatformState(routeStationStateOnTrip, towardsDest, node, cost);
            }

            // inc. board here since might be starting journey
            Iterable<Relationship> platformRelationships = node.getRelationships(OUTGOING,
                    BOARD, INTERCHANGE_BOARD, LEAVE_PLATFORM);
            // Cannot filter here as might be starting a new trip from this point, so need to 'go back' to the route station
            //Stream<Relationship> filterExcludingEndNode = filterExcludingEndNode(platformRelationships, routeStationStateOnTrip);
            return new PlatformState(routeStationStateOnTrip, platformRelationships, node, cost);
        }

        public TraversalState fromRouteStatiomEndTrip(RouteStationStateEndTrip routeStationState, Node node, int cost) {
            // towards final destination, just follow this one
            List<Relationship> towardsDest = routeStationState.traversalOps.getTowardsDestination(node.getRelationships(OUTGOING, LEAVE_PLATFORM));
            if (!towardsDest.isEmpty()) {
                return new PlatformState(routeStationState, towardsDest, node, cost);
            }

            Iterable<Relationship> platformRelationships = node.getRelationships(OUTGOING,
                    BOARD, INTERCHANGE_BOARD, LEAVE_PLATFORM);
            // end of a trip, may need to go back to this route station to catch new service
            return new PlatformState(routeStationState, platformRelationships, node, cost);
        }

    }

    private final Node platformNode;

    private PlatformState(TraversalState parent, Stream<Relationship> relationships, Node platformNode, int cost) {
        super(parent, relationships, cost);
        this.platformNode = platformNode;
    }

    private PlatformState(TraversalState parent, Iterable<Relationship> relationships, Node platformNode, int cost) {
        super(parent, relationships, cost);
        this.platformNode = platformNode;
    }

    @Override
    public String toString() {
        return "PlatformState{" +
                "platformNodeId=" + platformNode.getId() +
                "} " + super.toString();
    }

    @Override
    protected JustBoardedState toJustBoarded(JustBoardedState.Builder towardsJustBoarded, Node node, int cost, JourneyStateUpdate journeyState) {
        try {
            journeyState.board(TransportMode.Tram, platformNode, true);
        } catch (TramchesterException e) {
            throw new RuntimeException("unable to board tram", e);
        }
        return towardsJustBoarded.fromPlatformState(this, node, cost);
    }

    @Override
    protected TramStationState toTramStation(TramStationState.Builder towardsStation, Node node, int cost, JourneyStateUpdate journeyState) {
        return towardsStation.fromPlatform(this, node, cost);
    }

    @Override
    public long nodeId() {
        return platformNode.getId();
    }
}

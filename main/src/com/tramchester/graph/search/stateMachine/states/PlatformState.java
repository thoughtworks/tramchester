package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.search.JourneyState;
import com.tramchester.graph.search.stateMachine.NodeId;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.TowardsState;
import com.tramchester.graph.search.stateMachine.UnexpectedNodeTypeException;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class PlatformState extends TraversalState implements NodeId {

    public static class Builder implements TowardsState<PlatformState> {

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
            return new PlatformState(tramStationState,
                    node.getRelationships(OUTGOING, INTERCHANGE_BOARD, BOARD), node.getId(), cost);
        }

        public TraversalState fromRouteStationOnTrip(RouteStationStateOnTrip routeStationStateOnTrip, Node node, int cost) {

            // towards final destination, just follow this one
            List<Relationship> towardsDest = routeStationStateOnTrip.traversalOps.
                    getTowardsDestination(node.getRelationships(OUTGOING, LEAVE_PLATFORM));
            if (!towardsDest.isEmpty()) {
                return new PlatformState(routeStationStateOnTrip, towardsDest, node.getId(), cost);
            }

            Iterable<Relationship> platformRelationships = node.getRelationships(OUTGOING,
                    BOARD, INTERCHANGE_BOARD, LEAVE_PLATFORM);
            // filter so we don't just get straight back on tram if just boarded, or if we are on an existing trip
            Stream<Relationship> filterExcludingEndNode = filterExcludingEndNode(platformRelationships, routeStationStateOnTrip);
            return new PlatformState(routeStationStateOnTrip, filterExcludingEndNode, node.getId(), cost);
        }

        public TraversalState fromRouteStation(RouteStationStateEndTrip routeStationState, Node node, int cost) {
            // towards final destination, just follow this one
            List<Relationship> towardsDest = routeStationState.traversalOps.
                    getTowardsDestination(node.getRelationships(OUTGOING, LEAVE_PLATFORM));
            if (!towardsDest.isEmpty()) {
                return new PlatformState(routeStationState, towardsDest, node.getId(), cost);
            }

            Iterable<Relationship> platformRelationships = node.getRelationships(OUTGOING,
                    BOARD, INTERCHANGE_BOARD, LEAVE_PLATFORM);
            // end of a trip, may need to go back to this route station to catch new service
            return new PlatformState(routeStationState, platformRelationships, node.getId(), cost);
        }

    }

    private final long platformNodeId;

    private PlatformState(TraversalState parent, Stream<Relationship> relationships, long platformNodeId, int cost) {
        super(parent, relationships, cost);
        this.platformNodeId = platformNodeId;
    }

    private PlatformState(TraversalState parent, Iterable<Relationship> relationships, long platformNodeId, int cost) {
        super(parent, relationships, cost);
        this.platformNodeId = platformNodeId;
    }

    @Override
    public String toString() {
        return "PlatformState{" +
                "platformNodeId=" + platformNodeId +
                "} " + super.toString();
    }

    @Override
    public TraversalState createNextState(Set<GraphBuilder.Labels> nodeLabels, Node node, JourneyState journeyState, int cost) {
        // route station nodes may also have INTERCHANGE label set
        if (nodeLabels.contains(GraphBuilder.Labels.ROUTE_STATION)) {
            return toRouteStation(node, journeyState, cost);
        }
        throw new UnexpectedNodeTypeException(node, "Unexpected node type: "+nodeLabels);
    }

    @Override
    public TraversalState createNextState(GraphBuilder.Labels nodeLabel, Node node, JourneyState journeyState, int cost) {

//        long nodeId = node.getId();
//
//        if (nodeLabel == GraphBuilder.Labels.TRAM_STATION) {
//            if (traversalOps.isDestination(nodeId)) {
//                return builders.towardsDest(this).from(this, cost);
//            } else {
//                return builders.towardsStation(this).fromPlatform(this, node, cost);
//            }
//        }

        if (nodeLabel == GraphBuilder.Labels.ROUTE_STATION) {
            return toRouteStation(node, journeyState, cost);
        }

        throw new UnexpectedNodeTypeException(node, "Unexpected node type: "+nodeLabel);
    }

    private TraversalState toRouteStation(Node node, JourneyState journeyState, int cost) {
        try {
            journeyState.board(TransportMode.Tram);
        } catch (TramchesterException e) {
            throw new RuntimeException("unable to board tram", e);
        }

        return builders.towardsJustBoarded(this).
                fromPlatformState(this, node, cost);
    }

    @Override
    protected TramStationState toTramStation(TramStationState.Builder towardsStation, Node node, int cost, JourneyState journeyState) {
        return towardsStation.fromPlatform(this, node, cost);
    }

    @Override
    protected DestinationState toDestination(DestinationState.Builder towardsDestination, int cost) {
        return towardsDestination.from(this, cost);
    }

    @Override
    public long nodeId() {
        return platformNodeId;
    }
}

package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.input.Trip;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.search.JourneyState;
import com.tramchester.graph.search.stateMachine.*;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.tramchester.graph.TransportRelationshipTypes.LEAVE_PLATFORM;
import static java.lang.String.format;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class RouteStationStateOnTrip extends RouteStationTripState implements NodeId {
    private final long routeStationNodeId;
    private final IdFor<Trip> tripId;
    private final TransportMode transportMode;

    public static class Builder implements TowardsRouteStationTripState<RouteStationStateOnTrip> {

        public TraversalState fromMinuteState(MinuteState minuteState, Node node, int cost, Collection<Relationship> routeStationOutbound,
                                              IdFor<Trip> tripId) {
            TransportMode transportMode = GraphProps.getTransportMode(node);
            return new RouteStationStateOnTrip(minuteState, routeStationOutbound, cost, node.getId(), tripId, transportMode);
        }

        @Override
        public void register(RegistersFromState registers) {
            registers.add(MinuteState.class, this);
        }

        @Override
        public Class<RouteStationStateOnTrip> getDestination() {
            return RouteStationStateOnTrip.class;
        }
    }

    private RouteStationStateOnTrip(TraversalState parent, Iterable<Relationship> relationships, int cost,
                                    long routeStationNodeId, IdFor<Trip> tripId, TransportMode transportMode) {
        super(parent, relationships, cost);
        this.routeStationNodeId = routeStationNodeId;
        this.tripId = tripId;
        this.transportMode = transportMode;
    }

    @Override
    public String toString() {
        return "RouteStationStateOnTrip{" +
                "routeStationNodeId=" + routeStationNodeId +
                ", tripId=" + tripId +
                ", transportMode=" + transportMode +
                "} " + super.toString();
    }

    @Override
    public TraversalState createNextState(Set<GraphBuilder.Labels> nodeLabels, Node nextNode,
                                          JourneyState journeyState, int cost) {
        // should be called for multi-mode stations only
        return toStation(nextNode, journeyState, cost);
    }

    @Override
    public TraversalState createNextState(GraphBuilder.Labels nodeLabel, Node nextNode,
                                          JourneyState journeyState, int cost) {
        return switch (nodeLabel) {
            case PLATFORM -> toPlatform(nextNode, journeyState, cost);
            case SERVICE -> builders.towardsService(this).fromRouteStation(this, tripId, nextNode, cost);
            case BUS_STATION, TRAIN_STATION -> toStation(nextNode, journeyState, cost);
            default -> throw new UnexpectedNodeTypeException(nextNode, format("Unexpected node type: %s state :%s ", nodeLabel, this));
        };
    }

    private TraversalState toStation(Node stationNode, JourneyState journeyState, int cost) {
        leaveVehicle(journeyState, transportMode, "Unable to depart tram");

        long stationNodeId = stationNode.getId();
        if (traversalOps.isDestination(stationNodeId)) {
            return builders.towardsDest(this).from(this, cost);
        }

        return builders.towardsNoPlatformStation(this).fromRouteStation(this, stationNode, cost);
    }

    private TraversalState toPlatform(Node platformNode, JourneyState journeyState, int cost) {
        leaveVehicle(journeyState, TransportMode.Tram, "Unable to process platform");
        return builders.towardsPlatform(this).fromRouteStationOnTrip(this, platformNode, cost);
    }

    private void leaveVehicle(JourneyState journeyState, TransportMode transportMode, String s) {
        try {
            journeyState.leave(transportMode, getTotalCost());
        } catch (TramchesterException e) {
            throw new RuntimeException(s, e);
        }
    }

    @Override
    public long nodeId() {
        return routeStationNodeId;
    }

}

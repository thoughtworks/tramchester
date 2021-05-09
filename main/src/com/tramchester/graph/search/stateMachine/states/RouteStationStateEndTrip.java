package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.search.JourneyState;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.TowardsRouteStationTripState;
import com.tramchester.graph.search.stateMachine.TowardsState;
import com.tramchester.graph.search.stateMachine.UnexpectedNodeTypeException;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.List;
import java.util.Set;

import static com.tramchester.graph.TransportRelationshipTypes.LEAVE_PLATFORM;
import static java.lang.String.format;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class RouteStationStateEndTrip extends RouteStationTripState {

    @Override
    public String toString() {
        return "RouteStationStateEndTrip{" +
                "mode=" + mode +
                "} " + super.toString();
    }

    public static class Builder implements TowardsRouteStationTripState<RouteStationStateEndTrip> {

        public TraversalState fromMinuteState(MinuteState minuteState, int cost, List<Relationship> routeStationOutbound, TransportMode mode) {
            return new RouteStationStateEndTrip(minuteState, routeStationOutbound, cost, mode);
        }

        @Override
        public void register(RegistersFromState registers) {
            registers.add(MinuteState.class, this);
        }

        @Override
        public Class<RouteStationStateEndTrip> getDestination() {
            return RouteStationStateEndTrip.class;
        }
    }

    private final TransportMode mode;

    private RouteStationStateEndTrip(MinuteState minuteState, List<Relationship> routeStationOutbound, int cost, TransportMode mode) {
        super(minuteState, routeStationOutbound, cost);
        this.mode = mode;
    }

    @Override
    public TraversalState createNextState(Set<GraphBuilder.Labels> nodeLabel, Node nextNode,
                                          JourneyState journeyState, int cost) {
        // should only be called when we have multi-mode station
        return toStation(nextNode, journeyState, cost);
    }

    @Override
    public TraversalState createNextState(GraphBuilder.Labels nodeLabel, Node nextNode,
                                          JourneyState journeyState, int cost) {
        return switch (nodeLabel) {
            case PLATFORM -> toPlatform(nextNode, journeyState, cost);
            case SERVICE -> builders.towardsService(this).fromRouteStation(this, nextNode, cost);
            case BUS_STATION, TRAIN_STATION -> toStation(nextNode, journeyState, cost);
            default -> throw new UnexpectedNodeTypeException(nextNode, format("Unexpected node type: %s state :%s ", nodeLabel, this));
        };

    }

    private TraversalState toStation(Node nextNode, JourneyState journeyState, int cost) {
        // no platforms in bus network, direct to station
        try {
            journeyState.leave(mode, getTotalCost());
        } catch (TramchesterException e) {
            throw new RuntimeException("Unable to depart tram",e);
        }

        // if no platform station then may have arrived
        long nextNodeId = nextNode.getId();
        if (traversalOps.isDestination(nextNodeId)) {
            return builders.destination.from(this, cost);
        }

        return builders.towardsNoPlatformStation(this, NoPlatformStationState.class).
                fromRouteStation(this, nextNode, cost);
    }

    private TraversalState toPlatform(Node platformNode, JourneyState journeyState, int cost) {
        try {
            journeyState.leave(TransportMode.Tram, getTotalCost());
        }
        catch (TramchesterException exception) {
            throw new RuntimeException("Unable to process platform", exception);
        }

        // TODO Push into PlatformState
        // towards final destination, just follow this one
        List<Relationship> towardsDest = traversalOps.getTowardsDestination(platformNode.getRelationships(OUTGOING, LEAVE_PLATFORM));
        if (!towardsDest.isEmpty()) {
            return builders.platform.fromRouteStationTowardsDest(this, towardsDest, platformNode,  cost);
        }

        return builders.platform.fromRouteStation(this, platformNode, cost);

    }
}

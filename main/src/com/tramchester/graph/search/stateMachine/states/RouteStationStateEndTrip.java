package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.search.JourneyState;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.Towards;
import com.tramchester.graph.search.stateMachine.TowardsRouteStation;
import com.tramchester.graph.search.stateMachine.UnexpectedNodeTypeException;
import org.neo4j.graphdb.Entity;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.List;
import java.util.Set;

import static java.lang.String.format;

public class RouteStationStateEndTrip extends RouteStationState {

    @Override
    public String toString() {
        return "RouteStationStateEndTrip{" +
                "mode=" + mode +
                "} " + super.toString();
    }

    public static class Builder extends TowardsRouteStation<RouteStationStateEndTrip> {

        @Override
        public void register(RegistersFromState registers) {
            registers.add(MinuteState.class, this);
        }

        @Override
        public Class<RouteStationStateEndTrip> getDestination() {
            return RouteStationStateEndTrip.class;
        }

        public RouteStationStateEndTrip fromMinuteState(MinuteState minuteState, Entity node, int cost, Iterable<Relationship> routeStationOutbound) {
            TransportMode transportMode = GraphProps.getTransportMode(node);
            return new RouteStationStateEndTrip(minuteState, routeStationOutbound, cost, transportMode);
        }

    }

    private final TransportMode mode;

    private RouteStationStateEndTrip(MinuteState minuteState, Iterable<Relationship> routeStationOutbound, int cost, TransportMode mode) {
        super(minuteState, routeStationOutbound, cost);
        this.mode = mode;
    }

    @Override
    public TraversalState createNextState(Set<GraphBuilder.Labels> nodeLabels, Node nextNode,
                                          JourneyState journeyState, int cost) {
        // should only be called when we have multi-mode station
        //return toStation(nextNode, journeyState, cost);
        throw new UnexpectedNodeTypeException(nextNode, format("Unexpected node types: %s state :%s ", nodeLabels, this));
    }

    @Override
    public TraversalState createNextState(GraphBuilder.Labels nodeLabel, Node nextNode,
                                          JourneyState journeyState, int cost) {
        throw new UnexpectedNodeTypeException(nextNode, format("Unexpected node type: %s state :%s ", nodeLabel, this));
    }

    @Override
    protected TraversalState toService(ServiceState.Builder towardsService, Node node, int cost) {
        return towardsService.fromRouteStation(this, node, cost);
    }

    @Override
    protected TraversalState toNoPlatformStation(NoPlatformStationState.Builder towardsStation, Node node, int cost, JourneyState journeyState) {
        leaveVehicle(journeyState);
        return towardsStation.fromRouteStation(this, node, cost);
    }

    @Override
    protected TraversalState toPlatform(PlatformState.Builder towardsPlatform, Node node, int cost, JourneyState journeyState) {
        leaveVehicle(journeyState);
        return towardsPlatform.fromRouteStation(this, node, cost);
    }

    private TraversalState toStation(Node nextNode, JourneyState journeyState, int cost) {
        leaveVehicle(journeyState);

        // if no platform station then may have arrived
        long nextNodeId = nextNode.getId();
        if (traversalOps.isDestination(nextNodeId)) {
            return builders.towardsDest(this).from(this, cost);
        }

        return builders.towardsNoPlatformStation(this).fromRouteStation(this, nextNode, cost);
    }

    private void leaveVehicle(JourneyState journeyState) {
        try {
            journeyState.leave(mode, getTotalCost());
        } catch (TramchesterException e) {
            throw new RuntimeException("Unable to leave " + mode, e);
        }
    }
}

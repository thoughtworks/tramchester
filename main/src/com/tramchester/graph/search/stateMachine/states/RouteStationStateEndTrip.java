package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.OptionalResourceIterator;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.TowardsRouteStation;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

public class RouteStationStateEndTrip extends RouteStationState {

    @Override
    public String toString() {
        return "RouteStationStateEndTrip{" +
                "mode=" + mode +
                "} " + super.toString();
    }

    public static class Builder extends TowardsRouteStation<RouteStationStateEndTrip> {

        public Builder(boolean interchangesOnly) {
            super(interchangesOnly);
        }

        @Override
        public void register(RegistersFromState registers) {
            registers.add(TraversalStateType.MinuteState, this);
        }

        @Override
        public TraversalStateType getDestination() {
            return TraversalStateType.RouteStationStateEndTrip;
        }

        public RouteStationStateEndTrip fromMinuteState(MinuteState minuteState, Node node, Duration cost, boolean isInterchange, Trip trip) {
            TransportMode transportMode = GraphProps.getTransportMode(node);

            // TODO Crossing midnight?
            TramDate date = minuteState.traversalOps.getQueryDate();

            OptionalResourceIterator<Relationship> towardsDestination = getTowardsDestination(minuteState.traversalOps, node, date);
            if (!towardsDestination.isEmpty()) {
                // we've nearly arrived
                return new RouteStationStateEndTrip(minuteState, towardsDestination.stream(), cost, transportMode, node, trip, this);
            }

            Stream<Relationship> outboundsToFollow = getOutboundsToFollow(node, isInterchange, date);

            return new RouteStationStateEndTrip(minuteState, outboundsToFollow, cost, transportMode, node, trip, this);
        }

    }

    private final TransportMode mode;
    private final Node routeStationNode;
    private final Trip trip;

    private RouteStationStateEndTrip(MinuteState minuteState, Stream<Relationship> routeStationOutbound, Duration cost,
                                     TransportMode mode, Node routeStationNode, Trip trip, TowardsRouteStation<RouteStationStateEndTrip> builder) {
        super(minuteState, routeStationOutbound, cost, builder);
        this.mode = mode;
        this.routeStationNode = routeStationNode;
        this.trip = trip;
    }

    @Override
    protected TraversalState toService(ServiceState.Builder towardsService, Node node, Duration cost) {
        return towardsService.fromRouteStation(this, node, cost);
    }

    @Override
    protected TraversalState toNoPlatformStation(NoPlatformStationState.Builder towardsStation, Node node, Duration cost, JourneyStateUpdate journeyState, boolean onDiversion) {
        leaveVehicle(journeyState);
        return towardsStation.fromRouteStation(this, node, cost, journeyState, onDiversion);
    }

    @Override
    protected TraversalState toPlatform(PlatformState.Builder towardsPlatform, Node node, Duration cost, JourneyStateUpdate journeyState) {
        leaveVehicle(journeyState);
        return towardsPlatform.fromRouteStatiomEndTrip(this, node, cost);
    }

    private void leaveVehicle(JourneyStateUpdate journeyState) {
        try {
            journeyState.leave(trip.getId(), mode, getTotalDuration(), routeStationNode);
        } catch (TramchesterException e) {
            throw new RuntimeException("Unable to leave " + mode, e);
        }
    }
}

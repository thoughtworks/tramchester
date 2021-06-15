package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.TowardsRouteStation;
import org.geotools.data.store.EmptyIterator;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.List;
import java.util.Set;

import static com.tramchester.graph.TransportRelationshipTypes.DEPART;
import static com.tramchester.graph.TransportRelationshipTypes.INTERCHANGE_DEPART;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class RouteStationStateEndTrip extends RouteStationState {

    @Override
    public String toString() {
        return "RouteStationStateEndTrip{" +
                "mode=" + mode +
                "} " + super.toString();
    }

    public static class Builder extends TowardsRouteStation<RouteStationStateEndTrip> {

        private final boolean interchangesOnly;

        public Builder(boolean interchangesOnly) {
            this.interchangesOnly = interchangesOnly;
        }

        @Override
        public void register(RegistersFromState registers) {
            registers.add(MinuteState.class, this);
        }

        @Override
        public Class<RouteStationStateEndTrip> getDestination() {
            return RouteStationStateEndTrip.class;
        }

        public RouteStationStateEndTrip fromMinuteState(MinuteState minuteState, Node node, int cost, boolean isInterchange) {
            TransportMode transportMode = GraphProps.getTransportMode(node);

            Iterable<Relationship> allDeparts = node.getRelationships(OUTGOING, DEPART, INTERCHANGE_DEPART);

            List<Relationship> towardsDestination = minuteState.traversalOps.getTowardsDestination(allDeparts);
            if (!towardsDestination.isEmpty()) {
                // we've nearly arrived
                return new RouteStationStateEndTrip(minuteState, towardsDestination, cost, transportMode, node);
            }

            Iterable<Relationship> outboundsToFollow;
            if (interchangesOnly) {
                if (isInterchange) {
                    outboundsToFollow = node.getRelationships(OUTGOING, INTERCHANGE_DEPART);
                } else {
                    // trip ended here but not an interchange, can go no further
                    outboundsToFollow = EmptyIterator::new;
                }
            } else {
                outboundsToFollow = allDeparts;
            }

            return new RouteStationStateEndTrip(minuteState, outboundsToFollow, cost, transportMode, node);
        }

    }

    private final TransportMode mode;
    private final Node routeStationNode;

    private RouteStationStateEndTrip(MinuteState minuteState, Iterable<Relationship> routeStationOutbound, int cost,
                                     TransportMode mode, Node routeStationNode) {
        super(minuteState, routeStationOutbound, cost);
        this.mode = mode;
        this.routeStationNode = routeStationNode;
    }

    @Override
    protected TraversalState toService(ServiceState.Builder towardsService, Node node, int cost) {
        return towardsService.fromRouteStation(this, node, cost);
    }

    @Override
    protected TraversalState toNoPlatformStation(NoPlatformStationState.Builder towardsStation, Node node, int cost, JourneyStateUpdate journeyState) {
        leaveVehicle(journeyState);
        return towardsStation.fromRouteStation(this, node, cost);
    }

    @Override
    protected TraversalState toPlatform(PlatformState.Builder towardsPlatform, Node node, int cost, JourneyStateUpdate journeyState) {
        leaveVehicle(journeyState);
        return towardsPlatform.fromRouteStatiomEndTrip(this, node, cost);
    }

    private void leaveVehicle(JourneyStateUpdate journeyState) {
        try {
            journeyState.leave(mode, getTotalCost(), routeStationNode);
        } catch (TramchesterException e) {
            throw new RuntimeException("Unable to leave " + mode, e);
        }
    }
}

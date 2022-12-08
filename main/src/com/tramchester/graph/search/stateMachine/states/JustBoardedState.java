package com.tramchester.graph.search.stateMachine.states;

import com.google.common.collect.Streams;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.TowardsRouteStation;
import com.tramchester.graph.search.stateMachine.TraversalOps;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.time.Duration;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class JustBoardedState extends RouteStationState {

    public static class Builder extends TowardsRouteStation<JustBoardedState> {

        private final boolean depthFirst;

        public Builder(boolean depthFirst, boolean interchangesOnly) {
            super(interchangesOnly);
            this.depthFirst = depthFirst;
        }

        @Override
        public void register(RegistersFromState registers) {
            registers.add(TraversalStateType.PlatformState, this);
            registers.add(TraversalStateType.NoPlatformStationState, this);
        }

        @Override
        public TraversalStateType getDestination() {
            return TraversalStateType.JustBoardedState;
        }

        public JustBoardedState fromPlatformState(PlatformState platformState, Node node, Duration cost) {
            // does this ever happen? Get on one route station only to go back to a whole different
            // platform?
            Stream<Relationship> otherPlatforms = filterExcludingEndNode(node.getRelationships(OUTGOING, ENTER_PLATFORM),
                    platformState);

            Stream<Relationship> services;
            if (depthFirst) {
                services = orderServicesByRouteMetric(node, platformState.traversalOps);
            } else {
                services = Streams.stream(node.getRelationships(OUTGOING, TO_SERVICE));
            }

            return new JustBoardedState(platformState, Stream.concat(services, otherPlatforms), cost, this);
        }

        public JustBoardedState fromNoPlatformStation(NoPlatformStationState noPlatformStation, Node node, Duration cost) {
            Stream<Relationship> filteredDeparts = filterExcludingEndNode(node.getRelationships(OUTGOING, DEPART, INTERCHANGE_DEPART, DIVERSION_DEPART),
                    noPlatformStation);
            Stream<Relationship> services = orderServicesByRouteMetric(node, noPlatformStation.traversalOps);
            return new JustBoardedState(noPlatformStation, Stream.concat(filteredDeparts, services), cost, this);
        }

        /**
         * order by least number connections required to destination routes
         * @param node start node
         * @param traversalOps supporting ops
         * @return ordered by least number routes interconnects first
         */
        private Stream<Relationship> orderServicesByRouteMetric(Node node, TraversalOps traversalOps) {
            Iterable<Relationship> toServices = node.getRelationships(OUTGOING, TO_SERVICE);
            return traversalOps.orderBoardingRelationsByRouteConnections(toServices);
        }

        /***
         * Order outbound relationships by end node distance to destination
         * significant overall performance increase for non-trivial geographically diverse networks
         */
        private Stream<Relationship> orderServicesByDistance(Node node, TraversalOps traversalOps) {
            Iterable<Relationship> toServices = node.getRelationships(OUTGOING, TO_SERVICE);
            return traversalOps.orderRelationshipsByDistance(toServices);
        }

    }

    @Override
    public String toString() {
        return "RouteStationStateJustBoarded{} " + super.toString();
    }

    private JustBoardedState(TraversalState traversalState, Stream<Relationship> outbounds, Duration cost, TowardsRouteStation<?> builder) {
        super(traversalState, outbounds, cost, builder);
    }

    @Override
    protected TraversalState toService(ServiceState.Builder towardsService, Node node, Duration cost) {
        return towardsService.fromRouteStation(this, node, cost);
    }
}

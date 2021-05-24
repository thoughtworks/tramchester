package com.tramchester.graph.search.stateMachine.states;

import com.google.common.collect.Streams;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.TowardsRouteStation;
import com.tramchester.graph.search.stateMachine.TraversalOps;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class JustBoardedState extends RouteStationState {

    public static class Builder extends TowardsRouteStation<JustBoardedState> {

        @Override
        public void register(RegistersFromState registers) {
            registers.add(PlatformState.class, this);
            registers.add(NoPlatformStationState.class, this);
        }

        @Override
        public Class<JustBoardedState> getDestination() {
            return JustBoardedState.class;
        }

        public JustBoardedState fromPlatformState(PlatformState platformState, Node node, int cost) {
            Stream<Relationship> otherPlatforms = filterExcludingEndNode(node.getRelationships(OUTGOING, ENTER_PLATFORM),
                    platformState);
            Stream<Relationship> toServices = Streams.stream(node.getRelationships(OUTGOING, TO_SERVICE));
            return new JustBoardedState(platformState, Stream.concat(otherPlatforms, toServices), cost);
        }

        public JustBoardedState fromNoPlatformStation(NoPlatformStationState noPlatformStation, Node node, int cost) {
            Stream<Relationship> filteredDeparts = filterExcludingEndNode(node.getRelationships(OUTGOING, DEPART, INTERCHANGE_DEPART),
                    noPlatformStation);
            Stream<Relationship> services = orderServicesByDistance(node, noPlatformStation.traversalOps);
            return new JustBoardedState(noPlatformStation, Stream.concat(filteredDeparts, services), cost);
        }

        /***
         * Order outbound relationships by end node distance to destination
         * significant overall performance increase for non-trival geographically diverse networks
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

    private JustBoardedState(TraversalState traversalState, Stream<Relationship> outbounds, int cost) {
        super(traversalState, outbounds, cost);
    }

    @Override
    protected TraversalState toService(ServiceState.Builder towardsService, Node node, int cost) {
        return towardsService.fromRouteStation(this, node, cost);
    }
}

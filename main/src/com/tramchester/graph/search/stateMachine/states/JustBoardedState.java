package com.tramchester.graph.search.stateMachine.states;

import com.google.common.collect.Streams;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.search.JourneyState;
import com.tramchester.graph.search.stateMachine.*;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.*;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static java.lang.String.format;
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
         * follow those links that include a matching destination route first
         * Only useful for relatively simple routing with geographically close stops
         */
        private Iterable<Relationship> priortiseServicesByDestinationRoutes(TraversalState state, Node node) {
            Iterable<Relationship> toServices = node.getRelationships(OUTGOING, TO_SERVICE);
            ArrayList<Relationship> highPriority = new ArrayList<>();
            ArrayList<Relationship> lowPriority = new ArrayList<>();

            toServices.forEach(relationship -> {
                IdFor<Route> routeId = GraphProps.getRouteIdFrom(relationship);
                if (state.traversalOps.hasDestinationRoute(routeId)) {
                    highPriority.add(relationship);
                } else {
                    lowPriority.add(relationship);
                }
            });
            highPriority.addAll(lowPriority);
            return highPriority;
        }

        /***
         * Order outbound relationships by end node distance to destination
         * significant overall performance increase for non-trival gregraphically diverse networks
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
    public TraversalState createNextState(GraphBuilder.Labels nodeLabel, Node nextNode,
                                          JourneyState journeyState, int cost) {

        throw new UnexpectedNodeTypeException(nextNode, format("Unexpected node type: %s state :%s ", nodeLabel, this));
    }

    @Override
    protected TraversalState toService(ServiceState.Builder towardsService, Node node, int cost) {
        return towardsService.fromRouteStation(this, node, cost);
    }
}

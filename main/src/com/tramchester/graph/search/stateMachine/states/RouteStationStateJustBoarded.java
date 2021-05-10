package com.tramchester.graph.search.stateMachine.states;

import com.google.common.collect.Streams;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.search.JourneyState;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.TowardsState;
import com.tramchester.graph.search.stateMachine.TraversalOps;
import com.tramchester.graph.search.stateMachine.UnexpectedNodeTypeException;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.*;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static java.lang.String.format;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class RouteStationStateJustBoarded extends TraversalState {

    public static class Builder implements TowardsState<RouteStationStateJustBoarded> {

        public Builder() {
        }

        @Override
        public void register(RegistersFromState registers) {
            registers.add(PlatformState.class, this);
            registers.add(NoPlatformStationState.class, this);
        }

        @Override
        public Class<RouteStationStateJustBoarded> getDestination() {
            return RouteStationStateJustBoarded.class;
        }

        public TraversalState fromPlatformState(PlatformState platformState, Node node, int cost) {
            Stream<Relationship> outbounds = filterExcludingEndNode(node.getRelationships(OUTGOING, ENTER_PLATFORM),
                    platformState);
            Stream<Relationship> toServices = Streams.stream(node.getRelationships(OUTGOING, TO_SERVICE));
            return new RouteStationStateJustBoarded(platformState, Stream.concat(outbounds, toServices), cost);
        }

        public TraversalState fromNoPlatformStation(NoPlatformStationState noPlatformStation, Node node, int cost, TransportMode mode) {
            Stream<Relationship> filteredDeparts = filterExcludingEndNode(node.getRelationships(OUTGOING, DEPART, INTERCHANGE_DEPART),
                    noPlatformStation);

            Stream<Relationship> services;
            if (TransportMode.isTram(mode)) {
                services = Streams.stream(priortiseServicesByDestinationRoutes(noPlatformStation, node));
            } else {
                services = orderServicesByDistance(node, noPlatformStation.traversalOps);
            }

            return new RouteStationStateJustBoarded(noPlatformStation, Stream.concat(filteredDeparts, services), cost);
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

            return traversalOps.orderServicesByDistance(toServices);
//            Set<SortsPositions.HasStationId<Relationship>> relationships = new HashSet<>();
//
//            toServices.forEach(svcRelationship -> relationships.add(new RelationshipFacade(svcRelationship)));
//            return sortsPositions.sortedByNearTo(destinationLatLon, relationships);
        }

    }

    @Override
    public String toString() {
        return "RouteStationStateJustBoarded{} " + super.toString();
    }

    private RouteStationStateJustBoarded(TraversalState traversalState, Stream<Relationship> outbounds, int cost) {
        super(traversalState, outbounds, cost);
    }

    private RouteStationStateJustBoarded(TraversalState traversalState, Iterable<Relationship> outbounds, int cost) {
        super(traversalState, outbounds, cost);
    }

    @Override
    public TraversalState createNextState(GraphBuilder.Labels nodeLabel, Node nextNode,
                                          JourneyState journeyState, int cost) {

        if (nodeLabel == GraphBuilder.Labels.SERVICE) {
            return builders.towardsService(this).fromRouteStation(this, nextNode, cost);
        }

        // if one to one relationship between platforms and route stations, or bus stations and route stations,
        // no longer holds then this will throw
        throw new UnexpectedNodeTypeException(nextNode, format("Unexpected node type: %s state :%s ", nodeLabel, this));
    }


}

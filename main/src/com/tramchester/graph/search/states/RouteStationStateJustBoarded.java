package com.tramchester.graph.search.states;

import com.tramchester.domain.IdFor;
import com.tramchester.domain.Route;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.search.JourneyState;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import java.util.*;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static java.lang.String.format;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class RouteStationStateJustBoarded extends TraversalState {

    public static class Builder {
        private final SortsPositions sortsPositions;
        private final LatLong destinationLatLon;

        public Builder(SortsPositions sortsPositions, LatLong destinationLatLon) {
            this.sortsPositions = sortsPositions;
            this.destinationLatLon = destinationLatLon;
        }

        public TraversalState fromPlatformState(PlatformState platformState, Node node, int cost) {
            List<Relationship> outbounds = filterExcludingEndNode(node.getRelationships(OUTGOING, ENTER_PLATFORM),
                    platformState);
            node.getRelationships(OUTGOING, TO_SERVICE).forEach(outbounds::add);
            return new RouteStationStateJustBoarded(platformState, outbounds, cost);
        }

        public TraversalState fromNoPlatformStation(NoPlatformStationState noPlatformStation, Node node, int cost, TransportMode mode) {
            List<Relationship> outbounds = filterExcludingEndNode(node.getRelationships(OUTGOING, DEPART, INTERCHANGE_DEPART),
                    noPlatformStation);
            if (TransportMode.isBus(mode)) {
                outbounds.addAll(orderByDistance(node));
            } else {
                outbounds.addAll(orderByRoute(noPlatformStation, node));
            }

            return new RouteStationStateJustBoarded(noPlatformStation, outbounds, cost);
        }

        private Collection<Relationship> orderByRoute(TraversalState state, Node node) {
            Iterable<Relationship> toServices = node.getRelationships(OUTGOING, TO_SERVICE);
            ArrayList<Relationship> highPriority = new ArrayList<>();
            ArrayList<Relationship> lowPriority = new ArrayList<>();

            toServices.forEach(relationship -> {
                IdFor<Route> routeId = IdFor.getIdFrom(relationship,GraphStaticKeys.ROUTE_ID);
                if (state.destinationRoute(routeId)) {
                    highPriority.add(relationship);
                } else {
                    lowPriority.add(relationship);
                }
            });
            highPriority.addAll(lowPriority);
            return highPriority;
        }

        // significant overall performance increase
        private Collection<Relationship> orderByDistance(Node node) {
            Iterable<Relationship> toServices = node.getRelationships(OUTGOING, TO_SERVICE);

            Set<SortsPositions.HasStationId<Relationship>> relationships = new HashSet<>();
            toServices.forEach(svcRelationship -> relationships.add(new RelationshipFacade(svcRelationship)));
            return sortsPositions.sortedByNearTo(destinationLatLon, relationships);
        }
    }

    @Override
    public String toString() {
        return "RouteStationStateJustBoarded{} " + super.toString();
    }

    private RouteStationStateJustBoarded(TraversalState traversalState, Iterable<Relationship> outbounds, int cost) {
        super(traversalState, outbounds, cost);
    }

    @Override
    public TraversalState createNextState(Path path, GraphBuilder.Labels nodeLabel, Node nextNode,
                                          JourneyState journeyState, int cost) {

        if (nodeLabel == GraphBuilder.Labels.SERVICE) {
            return builders.service.fromRouteStation(this, nextNode, cost);
        }

        // if one to one relationship between platforms and route stations, or bus stations and route stations,
        // no longer holds then this will throw
        throw new RuntimeException(format("Unexpected node type: %s state :%s ", nodeLabel, this));
    }

    private static class RelationshipFacade implements SortsPositions.HasStationId<Relationship> {
        private final Relationship relationship;
        private final IdFor<Station> stationId;

        private RelationshipFacade(Relationship relationship) {
            this.relationship = relationship;
            this.stationId = IdFor.getIdFrom(relationship.getEndNode(),GraphStaticKeys.TOWARDS_STATION_ID);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            RelationshipFacade that = (RelationshipFacade) o;

            return stationId.equals(that.stationId);
        }

        @Override
        public int hashCode() {
            return stationId.hashCode();
        }

        @Override
        public IdFor<Station> getId() {
            return stationId;
        }

        @Override
        public Relationship getContained() {
            return relationship;
        }
    }
}

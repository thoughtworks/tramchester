package com.tramchester.graph.search.stateMachine;

import com.google.common.collect.Streams;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.search.RouteToRouteCosts;
import com.tramchester.repository.TripRepository;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.TO_SERVICE;

public class TraversalOps {
    private final NodeContentsRepository nodeOperations;
    private final TripRepository tripRepository;
    private final IdSet<Station> destinationStationIds;
    private final IdSet<Route> destinationRoutes;
    private final LatLong destinationLatLon;
    private final SortsPositions sortsPositions;
    private final RouteToRouteCosts routeToRouteCosts;

    // TODO Split into fixed and journey specific, inject fixed direct into builders
    public TraversalOps(NodeContentsRepository nodeOperations, TripRepository tripRepository,
                        SortsPositions sortsPositions, Set<Station> destinationStations,
                        LatLong destinationLatLon, RouteToRouteCosts routeToRouteCosts) {
        this.tripRepository = tripRepository;
        this.nodeOperations = nodeOperations;
        this.sortsPositions = sortsPositions;
        this.destinationStationIds = destinationStations.stream().collect(IdSet.collector());
        this.destinationRoutes = destinationStations.stream().
                flatMap(station -> station.getRoutes().stream()).
                collect(IdSet.collector());
        this.destinationLatLon = destinationLatLon;
        this.routeToRouteCosts = routeToRouteCosts;
    }

    public List<Relationship> getTowardsDestination(Iterable<Relationship> outgoing) {
        return getTowardsDestination(Streams.stream(outgoing));
    }

    private List<Relationship> getTowardsDestination(Stream<Relationship> outgoing) {
        return outgoing.
                filter(depart -> destinationStationIds.contains(GraphProps.getStationIdFrom(depart))).
                collect(Collectors.toList());
    }

    public int onDestRouteFirst(HasId<Route> a, HasId<Route> b) {
        IdFor<Route> routeA = a.getId();
        IdFor<Route> routeB = b.getId();
        boolean toDestA = destinationRoutes.contains(routeA);
        boolean toDestB = destinationRoutes.contains(routeB);
        if (toDestA == toDestB) {
            return 0;
        }
        if (toDestA) {
            return -1;
        }
        return 1;
    }

    public Stream<Relationship> orderRelationshipsByDistance(Iterable<Relationship> relationships) {
        Set<SortsPositions.HasStationId<Relationship>> wrapped = new HashSet<>();
        relationships.forEach(svcRelationship -> wrapped.add(new RelationshipFacade(svcRelationship)));
        return sortsPositions.sortedByNearTo(destinationLatLon, wrapped);
    }

    public Stream<Relationship> orderBoardingRelationsByDestRoute(Stream<Relationship> relationships) {
        return relationships.map(RelationshipWithRoute::new).
                sorted(this::onDestRouteFirst).
                map(RelationshipWithRoute::getRelationship);
    }

    public Stream<Relationship> orderBoardingRelationsByRouteConnections(Iterable<Relationship> toServices) {
        Stream<RelationshipWithRoute> withRouteId = Streams.stream(toServices).map(RelationshipWithRoute::new);
        Stream<RelationshipWithRoute> sorted = routeToRouteCosts.sortByDestinations(withRouteId, destinationRoutes);
        return sorted.map(RelationshipWithRoute::getRelationship);
    }

    public TramTime getTimeFrom(Node node) {
        return nodeOperations.getTime(node);
    }

    public Trip getTrip(IdFor<Trip> tripId) {
        return tripRepository.getTripById(tripId);
    }

    private boolean serviceNodeMatches(Relationship relationship, IdFor<Service> currentSvcId) {
        // TODO Add ServiceID to Service Relationship??
        Node svcNode = relationship.getEndNode();
        IdFor<Service> svcId = nodeOperations.getServiceId(svcNode);
        return currentSvcId.equals(svcId);
    }

    public boolean hasOutboundFor(Node node, IdFor<Service> serviceId) {
        return Streams.stream(node.getRelationships(Direction.OUTGOING, TO_SERVICE)).
                anyMatch(relationship -> serviceNodeMatches(relationship, serviceId));
    }

    private static class RelationshipFacade implements SortsPositions.HasStationId<Relationship> {
        private final Relationship relationship;
        private final Long id;
        private final IdFor<Station> stationId;

        private RelationshipFacade(Relationship relationship) {
            id = relationship.getId();
            this.relationship = relationship;

            // TODO this needs to go via the cache layer?
            this.stationId = GraphProps.getTowardsStationIdFrom(relationship.getEndNode());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            RelationshipFacade that = (RelationshipFacade) o;

            return id.equals(that.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public IdFor<Station> getStationId() {
            return stationId;
        }

        @Override
        public Relationship getContained() {
            return relationship;
        }
    }

    private static class RelationshipWithRoute implements HasId<Route> {
        private final Relationship relationship;
        private final IdFor<Route> routeId;

        public RelationshipWithRoute(Relationship relationship) {
            routeId = GraphProps.getRouteIdFrom(relationship);
            this.relationship = relationship;
        }

        public IdFor<Route> getRouteId() {
            return routeId;
        }

        public Relationship getRelationship() {
            return relationship;
        }

        @Override
        public GraphPropertyKey getProp() {
            return null;
        }

        @Override
        public IdFor<Route> getId() {
            return routeId;
        }
    }
}

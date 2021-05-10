package com.tramchester.graph.search.stateMachine;

import com.google.common.collect.Streams;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.search.stateMachine.states.TraversalStateFactory;
import com.tramchester.repository.TripRepository;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TraversalOps {
    private final NodeContentsRepository nodeOperations;
    private final TripRepository tripRepository;
    private final IdSet<Station> destinationStationIds;
    private final IdSet<Route> destinationRouteIds;
    private final Set<Long> destinationNodeIds;
    private final TraversalStateFactory builders;
    private final LatLong destinationLatLon;
    private final SortsPositions sortsPositions;

    public TraversalOps(NodeContentsRepository nodeOperations, TripRepository tripRepository,
                        SortsPositions sortsPositions, Set<Station> destinationStations, Set<Long> destinationNodeIds,
                        LatLong destinationLatLon, TraversalStateFactory traversalStateFactory) {
        this.tripRepository = tripRepository;
        this.nodeOperations = nodeOperations;
        this.sortsPositions = sortsPositions;
        this.destinationNodeIds = destinationNodeIds;
        this.destinationStationIds = destinationStations.stream().collect(IdSet.collector());
        this.destinationRouteIds = getDestinationRoutes(destinationStations);
        this.destinationLatLon = destinationLatLon;
        this.builders = traversalStateFactory;
    }

    private IdSet<Route> getDestinationRoutes(Set<Station> destinationStations) {
        return destinationStations.stream().map(Station::getRoutes).flatMap(Collection::stream).
                collect(IdSet.collector());
    }

    public List<Relationship> getTowardsDestination(Iterable<Relationship> outgoing) {
        return Streams.stream(outgoing).
                filter(depart -> destinationStationIds.contains(GraphProps.getStationIdFrom(depart))).
                collect(Collectors.toList());
    }

    public boolean hasDestinationRoute(IdFor<Route> routeId) {
        return destinationRouteIds.contains(routeId);
    }

    public TraversalStateFactory getBuilders() {
        return builders;
    }

    public boolean isDestination(long nodeId) {
        return destinationNodeIds.contains(nodeId);
    }

    public IdFor<Service> getServiceIdFor(IdFor<Trip> tripId) {
        return tripRepository.getTripById(tripId).getService().getId();
    }

    public IdFor<Service> getServiceIdFor(Node svcNode) {
        return nodeOperations.getServiceId(svcNode);
    }

    public Iterable<Relationship> filterBySingleTripId(Iterable<Relationship> relationships, IdFor<Trip> existingTripId) {
        return Streams.stream(relationships).
                filter(relationship -> nodeOperations.getTrip(relationship).equals(existingTripId)).
                collect(Collectors.toList());
    }

    public Stream<Relationship> orderServicesByDistance(Iterable<Relationship> relationships) {
        //Iterable<Relationship> toServices = node.getRelationships(OUTGOING, TO_SERVICE);

        Set<SortsPositions.HasStationId<Relationship>> wrapped = new HashSet<>();

        relationships.forEach(svcRelationship -> wrapped.add(new RelationshipFacade(svcRelationship)));
        return sortsPositions.sortedByNearTo(destinationLatLon, wrapped);
    }

    public TramTime getTimeFrom(Node node) {
        return nodeOperations.getTime(node);
    }

    private static class RelationshipFacade implements SortsPositions.HasStationId<Relationship> {
        private final Relationship relationship;
        private final Long id;
        private final IdFor<Station> stationId;

        private RelationshipFacade(Relationship relationship) {
            id = relationship.getId();
            this.relationship = relationship;

            // TODO this needs to go via the cache layer
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
}

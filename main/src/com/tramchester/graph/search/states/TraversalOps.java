package com.tramchester.graph.search.states;

import com.google.common.collect.Streams;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.repository.TripRepository;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TraversalOps {
    private final NodeContentsRepository nodeOperations;
    private final TripRepository tripRepository;
    private final IdSet<Station> destinationStationIds;
    private final IdSet<Route> destinationRouteIds;
    private final Set<Long> destinationNodeIds;
    private final TraversalState.Builders builders;

    public TraversalOps(NodeContentsRepository nodeOperations, TripRepository tripRepository,
                        Set<Station> destinationStations, Set<Long> destinationNodeIds, TraversalState.Builders builders) {
        this.tripRepository = tripRepository;
        this.nodeOperations = nodeOperations;
        this.destinationNodeIds = destinationNodeIds;
        this.destinationStationIds = destinationStations.stream().collect(IdSet.collector());
        this.destinationRouteIds = getDestinationRoutes(destinationStations);
        this.builders = builders;
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

    public TraversalState.Builders getBuilders() {
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

    public TramTime getTimeFrom(Node node) {
        return nodeOperations.getTime(node);
    }
}

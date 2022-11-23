package com.tramchester.graph.caches;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.NumberOfNodesAndRelationshipsRepository;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.metrics.CacheMetrics;
import com.tramchester.repository.ReportsCacheStats;
import org.apache.commons.lang3.tuple.Pair;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@LazySingleton
public class CachedNodeOperations implements ReportsCacheStats, NodeContentsRepository {
    private static final Logger logger = LoggerFactory.getLogger(CachedNodeOperations.class);

    private final Cache<Long, IdFor<Trip>> tripIdRelationshipCache;
    private final Cache<Long, IdFor<Service>> serviceNodeCache;
    private final Cache<Long, IdFor<Trip>> tripNodeCache;
    private final Cache<Long, IdFor<RouteStation>> routeStationIdCache;

    private final Cache<Long, Duration> relationshipCostCache;
    private final Cache<Long, TramTime> timeNodeCache;
    private final Cache<Long, Integer> hourNodeCahce;

    private final Cache<Long, EnumSet<GraphLabel>> labelCache;

    private final NumberOfNodesAndRelationshipsRepository numberOfNodesAndRelationshipsRepository;

    @Inject
    public CachedNodeOperations(CacheMetrics cacheMetrics, NumberOfNodesAndRelationshipsRepository numberOfNodesAndRelationshipsRepository) {
        this.numberOfNodesAndRelationshipsRepository = numberOfNodesAndRelationshipsRepository;

        relationshipCostCache = createCache("relationshipCostCache", numberFor(TransportRelationshipTypes.haveCosts()));
        tripIdRelationshipCache = createCache("tripIdRelationshipCache", numberFor(TransportRelationshipTypes.haveTripId()));
        routeStationIdCache = createCache("routeStationIdCache", GraphLabel.ROUTE_STATION);
        timeNodeCache = createCache("timeNodeCache", GraphLabel.MINUTE);
        serviceNodeCache = createCache("serviceNodeCache", GraphLabel.SERVICE);
        tripNodeCache = createCache("tripNodeCache", GraphLabel.MINUTE);
        hourNodeCahce = createCache("hourNodeCache", GraphLabel.HOUR);

        labelCache = Caffeine.newBuilder().maximumSize(50000).
                expireAfterAccess(10, TimeUnit.MINUTES).
                initialCapacity(40000).
                recordStats().build();

        cacheMetrics.register(this);
    }

    private Long numberFor(Set<TransportRelationshipTypes> types) {
        return types.stream().
                map(numberOfNodesAndRelationshipsRepository::numberOf).
                reduce(Long::sum).orElse(0L);
    }

    @SuppressWarnings("unused")
    @PreDestroy
    public void dispose() {
        logger.info("dispose");
        relationshipCostCache.invalidateAll();
        serviceNodeCache.invalidateAll();
        tripIdRelationshipCache.invalidateAll();
        timeNodeCache.invalidateAll();
        routeStationIdCache.invalidateAll();
        labelCache.invalidateAll();
        hourNodeCahce.invalidateAll();
    }

    @NonNull
    private <T> Cache<Long, T> createCache(String name, GraphLabel label) {
        return createCache(name, numberOfNodesAndRelationshipsRepository.numberOf(label));
    }

    @NonNull
    private <T> Cache<Long, T> createCache(String name, long maximumSize) {
        // TODO cache expiry time into Config
        logger.info("Create " + name + " max size " + maximumSize);
        return Caffeine.newBuilder().maximumSize(maximumSize).expireAfterAccess(30, TimeUnit.MINUTES).
                recordStats().build();
    }

    public List<Pair<String,CacheStats>> stats() {
        List<Pair<String,CacheStats>> result = new ArrayList<>();
        result.add(Pair.of("relationshipCostCache",relationshipCostCache.stats()));
        result.add(Pair.of("svcIdCache", serviceNodeCache.stats()));
        result.add(Pair.of("tripIdRelationshipCache", tripIdRelationshipCache.stats()));
        result.add(Pair.of("timeNodeCache", timeNodeCache.stats()));
        result.add(Pair.of("routeStationIdCache", routeStationIdCache.stats()));
        result.add(Pair.of("labelCache", labelCache.stats()));

        return result;
    }

    public IdFor<Trip> getTripId(Relationship relationship) {
        long relationshipId = relationship.getId();
        return tripIdRelationshipCache.get(relationshipId, id -> GraphProps.getTripId(relationship));
    }

    public TramTime getTime(Node node) {
        long nodeId = node.getId();
        return timeNodeCache.get(nodeId, id -> GraphProps.getTime(node));
    }

    @Override
    public IdFor<RouteStation> getRouteStationId(Node node) {
        long nodeId = node.getId();
        return routeStationIdCache.get(nodeId, id -> GraphProps.getRouteStationIdFrom(node));
    }

    public IdFor<Service> getServiceId(Node node) {
        long nodeId = node.getId();
        return serviceNodeCache.get(nodeId, id -> GraphProps.getServiceId(node));
    }

    @Override
    public IdFor<Trip> getTripId(Node node) {
        long nodeId = node.getId();
        return tripNodeCache.get(nodeId, id -> GraphProps.getTripId(node));
    }

    public int getHour(Node node) {
        long nodeId = node.getId();
        return hourNodeCahce.get(nodeId, id -> GraphLabel.getHourFrom(getLabels(node)));
        //return GraphLabel.getHourFrom(getLabels(node));
    }

    @Override
    public EnumSet<GraphLabel> getLabels(Node node) {
        long nodeId = node.getId();
        return labelCache.get(nodeId, id -> GraphProps.getLabelsFor(node));
    }

    @Override
    public Duration getCost(Relationship relationship) {
        TransportRelationshipTypes relationshipType = TransportRelationshipTypes.from(relationship);
        if (TransportRelationshipTypes.hasCost(relationshipType)) {
            long relationshipId = relationship.getId();
            return relationshipCostCache.get(relationshipId, id ->  GraphProps.getCost(relationship));
        } else {
            return Duration.ZERO;
        }
    }

    public void deleteFromCostCache(Relationship relationship) {
        long relationshipId = relationship.getId();
        relationshipCostCache.invalidate(relationshipId);
    }


}

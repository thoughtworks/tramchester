package com.tramchester.graph;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.Service;
import com.tramchester.domain.time.TramTime;
import com.tramchester.repository.ReportsCacheStats;
import org.apache.commons.lang3.tuple.Pair;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.picocontainer.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.tramchester.graph.GraphStaticKeys.*;

public class CachedNodeOperations implements ReportsCacheStats, Disposable, NodeContentsRepository {
    private static final Logger logger = LoggerFactory.getLogger(CachedNodeOperations.class);

    private final Cache<Long, Integer> relationshipCostCache;
    private final Cache<Long, String> tripRelationshipCache;
    private final Cache<Long, IdFor<Service>> svcIdCache;
    private final Cache<Long, Integer> hourNodeCache;
    private final Cache<Long, TramTime> times;

    public CachedNodeOperations() {
        // size tuned from stats
        relationshipCostCache = createCache(85000);
        svcIdCache = createCache(3000);
        hourNodeCache = createCache(38000);
        tripRelationshipCache = createCache(32500);
        times = createCache(40000);
    }

    @Override
    public void dispose() {
        logger.info("dispose");
        relationshipCostCache.invalidateAll();
        svcIdCache.invalidateAll();
        hourNodeCache.invalidateAll();
        tripRelationshipCache.invalidateAll();
        times.invalidateAll();
    }

    @NonNull
    private <T> Cache<Long, T> createCache(int maximumSize) {
        // TODO cache expiry time into Config
        return Caffeine.newBuilder().maximumSize(maximumSize).expireAfterAccess(30, TimeUnit.MINUTES).
                recordStats().build();
    }

    public List<Pair<String,CacheStats>> stats() {
        List<Pair<String,CacheStats>> result = new ArrayList<>();
        result.add(Pair.of("relationshipCostCache",relationshipCostCache.stats()));
        result.add(Pair.of("svcIdCache",svcIdCache.stats()));
        result.add(Pair.of("hourNodeCache",hourNodeCache.stats()));
        result.add(Pair.of("tripRelationshipCache", tripRelationshipCache.stats()));
        result.add(Pair.of("times", times.stats()));

        return result;
    }

    public String getTrips(Relationship relationship) {
        long relationshipId = relationship.getId();
        return tripRelationshipCache.get(relationshipId, id -> relationship.getProperty(TRIPS).toString());
    }

    public String getTrip(Relationship relationship) {
        long relationshipId = relationship.getId();
        return tripRelationshipCache.get(relationshipId, id -> relationship.getProperty(TRIP_ID).toString());
    }

    public TramTime getTime(Node node) {
        long nodeId = node.getId();
        return times.get(nodeId, id -> TramTime.of(((LocalTime) node.getProperty(TIME))));
    }

    public IdFor<Service> getServiceId(Node node) {
        long nodeId = node.getId();
        return svcIdCache.get(nodeId, id -> IdFor.getServiceIdFrom(node));
    }

    public int getHour(Node node) {
        long nodeId = node.getId();
        //noinspection ConstantConditions
        return hourNodeCache.get(nodeId, id -> (int) node.getProperty(GraphStaticKeys.HOUR));
    }

    public int getCost(Relationship relationship) {
        long relationshipId = relationship.getId();
        //noinspection ConstantConditions
        return relationshipCostCache.get(relationshipId, id ->  ((int) relationship.getProperty(GraphStaticKeys.COST)));
    }

    public void deleteFromCostCache(Relationship relationship) {
        long relationshipId = relationship.getId();
        relationshipCostCache.invalidate(relationshipId);
    }

}

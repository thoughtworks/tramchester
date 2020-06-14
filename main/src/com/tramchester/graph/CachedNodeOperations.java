package com.tramchester.graph;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.tramchester.domain.time.TramTime;
import com.tramchester.repository.ReportsCacheStats;
import org.apache.commons.lang3.tuple.Pair;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.picocontainer.Disposable;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.tramchester.graph.GraphStaticKeys.*;
import static com.tramchester.graph.GraphBuilder.Labels.BUS_STATION;
import static com.tramchester.graph.GraphBuilder.Labels.ROUTE_STATION;

public class CachedNodeOperations implements ReportsCacheStats, Disposable {

    private final Cache<Long, Integer> relationshipCostCache;
    private final Cache<Long, String> tripRelationshipCache;
    private final Cache<Long, String> svcIdCache;
    private final Cache<Long, Integer> hourNodeCache;

    // cached times
    private final Cache<Long, TramTime> times;
    // node types
    private final NodeIdLabelMap nodeIdLabelMap;

    public CachedNodeOperations(NodeIdLabelMap nodeIdLabelMap) {
        this.nodeIdLabelMap = nodeIdLabelMap;

        // size tuned from stats
        relationshipCostCache = createCache(85000);
        svcIdCache = createCache(3000);
        hourNodeCache = createCache(38000);
        tripRelationshipCache = createCache(32500);
        times = createCache(40000);
    }

    @Override
    public void dispose() {
        relationshipCostCache.invalidateAll();
        svcIdCache.invalidateAll();
        hourNodeCache.invalidateAll();
        tripRelationshipCache.invalidateAll();
        times.invalidateAll();
    }

    @NonNull
    private <T> Cache<Long, T> createCache(int maximumSize) {
        return Caffeine.newBuilder().maximumSize(maximumSize).expireAfterAccess(10, TimeUnit.MINUTES).
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

    public String getServiceId(Node node) {
        long id = node.getId();
        return svcIdCache.get(id, aLong -> node.getProperty(GraphStaticKeys.SERVICE_ID).toString());
    }

    public int getCost(Relationship relationship) {
        long relationshipId = relationship.getId();
        //noinspection ConstantConditions
        return relationshipCostCache.get(relationshipId, id ->  ((int) relationship.getProperty(GraphStaticKeys.COST)));
    }

    public int getHour(Node node) {
        long nodeId = node.getId();
        //noinspection ConstantConditions
        return hourNodeCache.get(nodeId, id -> (int) node.getProperty(GraphStaticKeys.HOUR));
    }

    public void deleteFromCache(Relationship relationship) {
        long relationshipId = relationship.getId();
        relationshipCostCache.invalidate(relationshipId);
    }

    public boolean isService(long nodeId) {
        return nodeIdLabelMap.has(GraphBuilder.Labels.SERVICE, nodeId);
    }

    public boolean isHour(long nodeId) {
        return nodeIdLabelMap.has(GraphBuilder.Labels.HOUR, nodeId);
    }

    public boolean isTime(long nodeId) {
        return nodeIdLabelMap.has(GraphBuilder.Labels.MINUTE, nodeId);
    }

    public boolean isRouteStation(long nodeId) {
        return nodeIdLabelMap.has(ROUTE_STATION, nodeId);
    }

    public boolean isBusStation(long nodeId) { return nodeIdLabelMap.has(BUS_STATION, nodeId); }

    // for creating query nodes, to support MyLocation journeys
    public Node createQueryNode(GraphDatabase graphDatabase, Transaction txn) {
        Node result = graphDatabase.createNode(txn, GraphBuilder.Labels.QUERY_NODE);
        nodeIdLabelMap.putQueryNode(result.getId());
        return result;
    }

    // for deleting query nodes, to support MyLocation journeys
    public void deleteNode(Node node) {
        long id = node.getId();
        node.delete();
        nodeIdLabelMap.removeQueryNode(id);
    }

}

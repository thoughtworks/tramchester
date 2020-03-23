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

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.tramchester.graph.GraphStaticKeys.*;
import static com.tramchester.graph.TransportGraphBuilder.Labels.ROUTE_STATION;

public class CachedNodeOperations implements ReportsCacheStats {

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

    @NonNull
    private <T> Cache<Long, T> createCache(int maximumSize) {
        return Caffeine.newBuilder().maximumSize(maximumSize).expireAfterAccess(10, TimeUnit.MINUTES).recordStats().build();
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
        String ifPresent = tripRelationshipCache.getIfPresent(relationshipId);
        if (ifPresent!=null) {
            return ifPresent;
        }
        String trips = relationship.getProperty(TRIPS).toString();
        tripRelationshipCache.put(relationshipId, trips);
        return trips;
    }

    public String getTrip(Relationship relationship) {
        long relationshipId = relationship.getId();
        String ifPresent = tripRelationshipCache.getIfPresent(relationshipId);
        if (ifPresent!=null) {
            return ifPresent;
        }
        String trip = relationship.getProperty(TRIP_ID).toString().intern();
        tripRelationshipCache.put(relationshipId, trip);
        return trip;
    }

    public String getTrip(Node endNode) {
        if (!endNode.hasProperty(TRIP_ID)) {
            return "";
        }
        return endNode.getProperty(TRIP_ID).toString().intern();
    }

    public TramTime getTime(Node node) {
        long nodeId = node.getId();
        TramTime ifPresent = times.getIfPresent(nodeId);
        if (ifPresent!=null) {
            return ifPresent;
        }
        LocalTime value = (LocalTime) node.getProperty(TIME);
        TramTime tramTime = TramTime.of(value);
        times.put(nodeId,tramTime);
        return tramTime;
    }

    public String getServiceId(Node node) {
        long id = node.getId();
        String ifPresent = svcIdCache.getIfPresent(id);
        if (ifPresent!=null) {
            return ifPresent;
        }
        String svcId = node.getProperty(GraphStaticKeys.SERVICE_ID).toString().intern();
        svcIdCache.put(id, svcId);
        return svcId;
    }

    public int getCost(Relationship relationship) {
        long relationshipId = relationship.getId();
        Integer ifPresent = relationshipCostCache.getIfPresent(relationshipId);
        if (ifPresent!=null) {
            return ifPresent;
        }

        int cost = (int) relationship.getProperty(GraphStaticKeys.COST);
        relationshipCostCache.put(relationshipId,cost);
        return cost;
    }

    public void deleteFromCache(Relationship relationship) {
        long relationshipId = relationship.getId();
        relationshipCostCache.invalidate(relationshipId);
//        if (relationshipCostCache.containsKey(relationshipId)) {
//            relationshipCostCache.remove(relationshipId);
//        }
    }

    public boolean isService(long nodeId) {
        return nodeIdLabelMap.has(TransportGraphBuilder.Labels.SERVICE, nodeId);
    }

    public boolean isHour(long nodeId) {
        return nodeIdLabelMap.has(TransportGraphBuilder.Labels.HOUR, nodeId);
    }

    public boolean isTime(long nodeId) {
        return nodeIdLabelMap.has(TransportGraphBuilder.Labels.MINUTE, nodeId);
    }

    public boolean isRouteStation(long nodeId) {
        return nodeIdLabelMap.has(ROUTE_STATION, nodeId);
    }

    public int getHour(Node node) {
        long id = node.getId();
        Integer ifPresent = hourNodeCache.getIfPresent(id);
        if (ifPresent!=null) {
            return ifPresent;
        }
        int hour = (int) node.getProperty(GraphStaticKeys.HOUR);
        hourNodeCache.put(id,hour);
        return hour;
    }

    // for creating query nodes, to support MyLocation joruneys
    public Node createQueryNode(NodeIdQuery nodeIdQuery) {
        Node result = nodeIdQuery.createNode(TransportGraphBuilder.Labels.QUERY_NODE);
        nodeIdLabelMap.putQueryNode(result.getId());
        return result;
    }

    // for deleting query nodes, to support MyLocation joruneys
    public void deleteNode(Node node) {
        long id = node.getId();
        node.delete();
        nodeIdLabelMap.removeQueryNode(id);
    }

}

package com.tramchester.graph;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.repository.ReportsCacheStats;
import org.apache.commons.lang3.tuple.Pair;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

public final class NodeIdQuery implements ReportsCacheStats {
    private static final Logger logger = LoggerFactory.getLogger(NodeIdQuery.class);

    private final List<CacheOfNodes> caches;
    private final CacheOfNodes routeStationNodeCache;
    private final CacheOfNodes platformNodeCache;
    private final CacheOfNodes hourNodeCache;
    private final CacheOfNodes serviceNodeCache;
    private final CacheOfNodes tramStationNodeCache;
    private final CacheOfNodes busStationNodeCache;

    private final GraphQuery graphQuery;
    private final boolean buses;
    // TODO remove
    private final boolean warnIfMissing = false;

    public NodeIdQuery(GraphQuery graphQuery, TramchesterConfig config) {
        this.graphQuery = graphQuery;
        this.buses = config.getBus();

        caches = new ArrayList<>();
        // tuned via stats
        routeStationNodeCache = createCache(11000, "routeStationNodeCache", GraphQuery::getRouteStationNode);
        platformNodeCache = createCache(500, "platformNodeCache", GraphQuery::getPlatformNode);
        hourNodeCache = createCache(50000, "hourNodeCache", GraphQuery::getHourNode);
        serviceNodeCache = createCache(11000, "serviceNodeCache", GraphQuery::getServiceNode);
        tramStationNodeCache = createCache(200, "tramStationNodeCache", GraphQuery::getTramStationNode);

        if (buses) {
            // TODO tune from stats
            busStationNodeCache = createCache(10000, "busStationNodeCache", GraphQuery::getBusStationNode);
        } else {
            busStationNodeCache = null;
        }
    }


//    public Node createNode(TransportGraphBuilder.Labels labels) {
//        return graphDatabaseService.createNode(labels);
//    }

    private CacheOfNodes createCache(long maximumSize, String routeStationNodeCache, FindNode getRouteStationNode) {
        CacheOfNodes cache = new CacheOfNodes(routeStationNodeCache, getRouteStationNode, maximumSize);
        caches.add(cache);
        return cache;
    }

    protected Node getRouteStationNode(String routeStationId) {
        return routeStationNodeCache.getNode(routeStationId);
    }

    public Node getPlatformNode(String platformId) {
        return platformNodeCache.getNode(platformId);
    }

    public Node getServiceNode(String svcId) {
        return serviceNodeCache.getNode(svcId);
    }

    public Node getHourNode(String hourId) {
        return hourNodeCache.getNode(hourId);
    }


    public Node getTramStationNode(String stationId) {
        return tramStationNodeCache.getNode(stationId);
    }

    public Node getBusStationNode(String stationId) {
        return busStationNodeCache.getNode(stationId);
    }

    public Node getStationNode(String stationId) {
        if (buses) {
            return getStationNodeForEither(stationId);
        }
        return tramStationNodeCache.getNode(stationId);
    }

    // ASSUME: stationId uniqueness
    private Node getStationNodeForEither(String stationId) {
        // performance
        if (tramStationNodeCache.has(stationId)) {
            return tramStationNodeCache.getNode(stationId);
        }
        if (busStationNodeCache.has(stationId)) {
            return busStationNodeCache.getNode(stationId);
        }

        // neither cached at this point
        Node tramNode = tramStationNodeCache.getNode(stationId);
        if (tramNode!=null) {
            return tramNode;
        }

        Node busNode = graphQuery.getBusStationNode(stationId);
        if (busNode!=null) {
            return busNode;
        }

        if (warnIfMissing) {
            logger.warn(format("Could not find graph node for station: '%s'", stationId));
        }
        return null;
    }

    public Node getAreaNode(String areaName) {
        // TODO Cache
        Node node = graphQuery.getAreaNode(areaName);
        return node;
    }

    @Override
    public List<Pair<String, CacheStats>> stats() {
        List<Pair<String,CacheStats>> result = new ArrayList<>();
        caches.forEach(cache -> result.add(cache.reportStats()));
        return result;
    }

    // TODO split this class into two, clear caches only used during graph rebuild to reduce memory footprint
    public void clearAfterGraphBuild() {
        routeStationNodeCache.clear();
        platformNodeCache.clear();
        hourNodeCache.clear();
        serviceNodeCache.clear();
    }

    private interface FindNode {
        Node find(GraphQuery query, String id);
    }

    private class CacheOfNodes {
        private final Cache<String,Node> theCache;
        private String name;
        private final FindNode findNode;

        public CacheOfNodes(String name, FindNode findNode, long maximumSize) {
            this.name = name;
            this.findNode = findNode;
            theCache = Caffeine.newBuilder().maximumSize(maximumSize).
                    expireAfterAccess(10, TimeUnit.MINUTES).recordStats().build();
        }

        public Node getNode(String nodeId) {
            Node ifPresent = theCache.getIfPresent(nodeId);
            if (ifPresent!=null) {
                return ifPresent;
            }
            Node node = findNode.find(graphQuery, nodeId);
            if (node!=null) {
                theCache.put(nodeId, node);
            } else if (warnIfMissing) {
                logger.warn(format("Could not find graph node for: '%s'", nodeId));
            }
            return node;
        }

        public boolean has(String stationId) {
            return theCache.getIfPresent(stationId)!=null;
        }

        public Pair<String, CacheStats> reportStats() {
            return Pair.of(name, theCache.stats());
        }

        public void clear() {
            theCache.invalidateAll();
        }
    }
}

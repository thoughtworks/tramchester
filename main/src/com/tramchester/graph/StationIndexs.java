package com.tramchester.graph;

import com.tramchester.config.TramchesterConfig;
import org.neo4j.gis.spatial.SimplePointLayer;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.lang.String.format;

public final class StationIndexs {
    private static final Logger logger = LoggerFactory.getLogger(StationIndexs.class);

    private final CacheOfNodes routeStationNodeCache;
    private final CacheOfNodes platformNodeCache;
    private final CacheOfNodes minuteNodeCache;
    private final CacheOfNodes hourNodeCache;
    private final CacheOfNodes serviceNodeCache;
    private final CacheOfNodes tramStationNodeCache;
    private final CacheOfNodes busStationNodeCache;

    private SimplePointLayer spatialLayer;

    private final GraphDatabaseService graphDatabaseService;
    private final GraphQuery graphQuery;
    private final boolean buses;
    // TODO remove
    private final boolean warnIfMissing = false;

    public StationIndexs(GraphDatabaseService graphDatabaseService, GraphQuery graphQuery, TramchesterConfig config) {
        this.graphDatabaseService = graphDatabaseService;
        this.graphQuery = graphQuery;
        this.buses = config.getBus();

        routeStationNodeCache = new CacheOfNodes(GraphQuery::getRouteStationNode);
        platformNodeCache = new CacheOfNodes(GraphQuery::getPlatformNode);
        minuteNodeCache = new CacheOfNodes(GraphQuery::getTimeNode);
        hourNodeCache = new CacheOfNodes(GraphQuery::getHourNode);
        serviceNodeCache = new CacheOfNodes(GraphQuery::getServiceNode);

        tramStationNodeCache = new CacheOfNodes(GraphQuery::getTramStationNode);
        if (buses) {
            busStationNodeCache = new CacheOfNodes(GraphQuery::getBusStationNode);
        } else {
            busStationNodeCache = null;
        }
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

    public Node getTimeNode(String timeId) {
        return minuteNodeCache.getNode(timeId);
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

    protected SimplePointLayer getSpatialLayer() {
        if (spatialLayer == null) {
            spatialLayer = graphQuery.getSpatialLayer();
        }
        return spatialLayer;
    }

    public Node createNode(TransportGraphBuilder.Labels labels) {
        return graphDatabaseService.createNode(labels);
    }


    private interface FindNode {
        Node find(GraphQuery query, String id);
    }

    private class CacheOfNodes {
        private final ConcurrentMap<String,Node> theCache;
        private final FindNode findNode;

        public CacheOfNodes(FindNode findNode) {
            this.findNode = findNode;
            theCache = new ConcurrentHashMap<>();
        }

        public Node getNode(String nodeId) {
            if (theCache.containsKey(nodeId)) {
                return theCache.get(nodeId);
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
            return theCache.containsKey(stationId);
        }
    }
}

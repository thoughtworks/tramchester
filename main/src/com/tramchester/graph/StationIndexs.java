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
    
    private final ConcurrentMap<String,Node> tramStationNodeCache;
    private final ConcurrentMap<String,Node> busStationNodeCache;

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

        tramStationNodeCache = new ConcurrentHashMap<>();
        if (buses) {
            busStationNodeCache = new ConcurrentHashMap<>();
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

    public Node getStationNode(String stationId) {
        if (buses) {
            return getStationNodeForEither(stationId);
        }

        return getNode(tramStationNodeCache, GraphQuery::getTramStationNode, stationId);
    }

    private Node getNode(ConcurrentMap<String, Node> nodeCache, FindNode findNode, String nodeId) {
        if (nodeCache.containsKey(nodeId)) {
            return nodeCache.get(nodeId);
        }
        Node node = findNode.find(graphQuery, nodeId);
        if (node!=null) {
            nodeCache.put(nodeId, node);
        } else if (warnIfMissing) {
            logger.warn(format("Could not find graph node for: '%s'", nodeId));
        }
        return node;
    }

    // ASSUME: stationId uniqueness
    private Node getStationNodeForEither(String stationId) {
        if (tramStationNodeCache.containsKey(stationId)) {
            return tramStationNodeCache.get(stationId);
        }
        if (busStationNodeCache.containsKey(stationId)) {
            return busStationNodeCache.get(stationId);
        }

        Node tramNode = graphQuery.getTramStationNode(stationId);
        if (tramNode!=null) {
            tramStationNodeCache.put(stationId, tramNode);
            return tramNode;
        }

        Node busNode = graphQuery.getBusStationNode(stationId);
        if (busNode!=null) {
            busStationNodeCache.put(stationId, busNode);
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

    public Node getServiceNode(String svcNodeId) {
        return serviceNodeCache.getNode(svcNodeId);
    }

    public Node getHourNode(String hourNodeId) {
        return hourNodeCache.getNode(hourNodeId);
    }

    public Node getTimeNode(String timeNodeId) {
        return minuteNodeCache.getNode(timeNodeId);
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

    }
}

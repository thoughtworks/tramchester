package com.tramchester.graph;

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

    // TODO with the indexes is the cache needed?
    private final ConcurrentMap<String,Node> routeStationNodeCache;
    private final ConcurrentMap<String,Node> stationNodeCache;
    private final ConcurrentMap<String,Node> platformNodeCache;
    private SimplePointLayer spatialLayer;

    protected final GraphDatabaseService graphDatabaseService;
    protected final GraphQuery graphQuery;
    private final boolean warnIfMissing = false;

    public StationIndexs(GraphDatabaseService graphDatabaseService, GraphQuery graphQuery) {

        this.graphDatabaseService = graphDatabaseService;
        this.graphQuery = graphQuery;
        routeStationNodeCache = new ConcurrentHashMap<>();
        stationNodeCache = new ConcurrentHashMap<>();
        platformNodeCache = new ConcurrentHashMap<>();
    }

    protected Node getRouteStationNode(String routeStationId) {
        if (routeStationNodeCache.containsKey(routeStationId)) {
            return routeStationNodeCache.get(routeStationId);
        }
        Node node = graphQuery.getRouteStationNode(routeStationId);
        if (node!=null) {
            routeStationNodeCache.put(routeStationId, node);
        } else if (warnIfMissing) {
            logger.warn(format("Could not find graph node for route station: '%s'", routeStationId));
        }
        return node;
    }

    public Node getStationNode(String stationId) {
        if (stationNodeCache.containsKey(stationId)) {
            return stationNodeCache.get(stationId);
        }
        Node node = graphQuery.getStationNode(stationId);
        if (node!=null) {
            stationNodeCache.put(stationId, node);
        }
        else if (warnIfMissing) {
            logger.warn(format("Could not find graph node for station: '%s'", stationId));
        }
        return node;
    }

    public Node getPlatformNode(String id) {
        if (platformNodeCache.containsKey(id)) {
            return platformNodeCache.get(id);
        }
        Node node = graphQuery.getPlatformNode(id);
        if (node!=null) {
            platformNodeCache.put(id,node);
        } else if (warnIfMissing) {
            logger.warn(format("Could not find graph node for platform: '%s'", id));
        }
        return node;
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
}

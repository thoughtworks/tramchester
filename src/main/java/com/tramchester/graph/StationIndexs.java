package com.tramchester.graph;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class StationIndexs {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(StationIndexs.class);
    private Map<String,Node> routeStationNodeCache;
    private Map<String,Node> stationNodeCache;
    private Index<Node> spatialIndex;

    protected GraphDatabaseService graphDatabaseService;
    protected GraphQuery graphQuery;
    private boolean warnIfMissing;

    public StationIndexs(GraphDatabaseService graphDatabaseService, boolean warnIfMissing) {
        this.graphDatabaseService = graphDatabaseService;
        graphQuery = new GraphQuery(graphDatabaseService);
        this.warnIfMissing = warnIfMissing;
        routeStationNodeCache = new HashMap<>();
        stationNodeCache = new HashMap<>();
    }

    protected Node getRouteStationNode(String routeStationId) {
        if (routeStationNodeCache.containsKey(routeStationId)) {
            return routeStationNodeCache.get(routeStationId);
        }
        Node node = graphQuery.getRouteStationNode(routeStationId);
        if (node!=null) {
            routeStationNodeCache.put(routeStationId, node);
        } else if (warnIfMissing) {
            logger.warn("Could not find graph node for route station: " + routeStationId);
        }
        return node;
    }

    protected Node getStationNode(String stationId) {
        if (stationNodeCache.containsKey(stationId)) {
            return stationNodeCache.get(stationId);
        }
        Node node = graphQuery.getStationNode(stationId);
        if (node!=null) {
            stationNodeCache.put(stationId, node);
        }
        else if (warnIfMissing) {
            logger.warn("Could not find graph node for station: " + stationId);
        }
        return node;
    }

    protected Index<Node> getSpatialIndex() {
        if (spatialIndex == null) {
            spatialIndex = graphQuery.getSpatialIndex();
        }
        return spatialIndex;
    }
}

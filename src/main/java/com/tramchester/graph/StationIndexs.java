package com.tramchester.graph;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class StationIndexs {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(StationIndexs.class);
    private Map<String,Node> routeStationNodeCache;
    private Map<String,Node> stationNodeCache;


    protected GraphDatabaseService graphDatabaseService;
    private boolean warnIfMissing;

    public StationIndexs(GraphDatabaseService graphDatabaseService, boolean warnIfMissing) {
        this.graphDatabaseService = graphDatabaseService;
        this.warnIfMissing = warnIfMissing;
        routeStationNodeCache = new HashMap<>();
        stationNodeCache = new HashMap<>();
    }

    protected Node getRouteStationNode(String routeStationId) {
        if (routeStationNodeCache.containsKey(routeStationId)) {
            return routeStationNodeCache.get(routeStationId);
        }
        Node node = graphDatabaseService.findNode(DynamicLabel.label(TransportGraphBuilder.ROUTE_STATION), GraphStaticKeys.ID, routeStationId);
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
        Node node = graphDatabaseService.findNode(DynamicLabel.label(TransportGraphBuilder.STATION), GraphStaticKeys.ID, stationId);
        if (node!=null) {
            stationNodeCache.put(stationId, node);
        }
        else if (warnIfMissing) {
            logger.warn("Could not find graph node for station: " + stationId);
        }
        return node;
    }
}

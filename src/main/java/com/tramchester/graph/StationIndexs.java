package com.tramchester.graph;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.slf4j.LoggerFactory;

public class StationIndexs {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(StationIndexs.class);

    protected GraphDatabaseService graphDatabaseService;

    public StationIndexs(GraphDatabaseService graphDatabaseService) {
        this.graphDatabaseService = graphDatabaseService;
    }

    protected Node getRouteStationNode(String routeStationId) {
        logger.info("Find station with id: " + routeStationId);
        Node node = graphDatabaseService.findNode(DynamicLabel.label(TransportGraphBuilder.ROUTE_STATION), GraphStaticKeys.ID, routeStationId);
        if (node==null) {
            logger.warn("Could not find graph node for route station: " + routeStationId);
        }
        return node;
    }

    protected Node getStationNode(String stationId) {
        logger.info("Find station with id: " + stationId);
        Node node = graphDatabaseService.findNode(DynamicLabel.label(TransportGraphBuilder.STATION), GraphStaticKeys.ID, stationId);
        if (node==null) {
            logger.warn("Could not find graph node for station: " + stationId);
        }
        return node;
    }
}

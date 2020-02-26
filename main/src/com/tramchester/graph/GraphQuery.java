package com.tramchester.graph;

import org.neo4j.gis.spatial.SimplePointLayer;
import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.gis.spatial.encoders.SimplePointEncoder;
import org.neo4j.graphdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class GraphQuery {

    // TODO REFACTOR Methods only used for tests into own class
    private static final Logger logger = LoggerFactory.getLogger(GraphQuery.class);

    private SpatialDatabaseService spatialDatabaseService;
    private GraphDatabaseService graphDatabaseService;

    public GraphQuery(GraphDatabaseService graphDatabaseService, SpatialDatabaseService spatialDatabaseService) {
        this.graphDatabaseService = graphDatabaseService;
        this.spatialDatabaseService = spatialDatabaseService;
    }

    public Node getTramStationNode(String stationId) {
        return getNodeByLabel(stationId, TransportGraphBuilder.Labels.TRAM_STATION);
    }

    public Node getBusStationNode(String stationId) {
        return getNodeByLabel(stationId, TransportGraphBuilder.Labels.BUS_STATION);
    }

    public Node getPlatformNode(String id) {
        return getNodeByLabel(id, TransportGraphBuilder.Labels.PLATFORM);
    }

    public Node getServiceNode(String id) {
        TransportGraphBuilder.Labels Label = TransportGraphBuilder.Labels.SERVICE;
        return getNodeByLabel(id, Label);
    }

    public Node getAreaNode(String id) {
        return getNodeByLabel(id, TransportGraphBuilder.Labels.AREA);
    }

    public Node getRouteStationNode(String id) {
        return getNodeByLabel(id, TransportGraphBuilder.Labels.ROUTE_STATION);
    }

    public Node getTimeNode(String id) {
        return getNodeByLabel(id, TransportGraphBuilder.Labels.MINUTE);
    }

    public Node getHourNode(String id) {
        return getNodeByLabel(id, TransportGraphBuilder.Labels.HOUR);
    }

    private Node getNodeByLabel(String id, TransportGraphBuilder.Labels label) {
        return graphDatabaseService.findNode(label, GraphStaticKeys.ID, id);
    }

    public SimplePointLayer getSpatialLayer() {
        return (SimplePointLayer) spatialDatabaseService.getOrCreateLayer("stations",
                SimplePointEncoder.class, SimplePointLayer.class);
    }

    public List<Relationship> getRouteStationRelationships(String routeStationId, Direction direction) {
        Node routeStationNode = getRouteStationNode(routeStationId);
        if (routeStationNode==null) {
            return Collections.emptyList();
        }
        List<Relationship> result = new LinkedList<>();
        routeStationNode.getRelationships(direction, TransportRelationshipTypes.forPlanning()).forEach(result::add);
        return result;
    }

}

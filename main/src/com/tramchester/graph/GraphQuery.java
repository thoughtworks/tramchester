package com.tramchester.graph;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class GraphQuery {

    // TODO REFACTOR Methods only used for tests into own class
    private static final Logger logger = LoggerFactory.getLogger(GraphQuery.class);

    private GraphDatabase graphDatabase;

    public GraphQuery(GraphDatabase graphDatabase) {
        this.graphDatabase = graphDatabase;
        //this.spatialDatabaseService = spatialDatabaseService;
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
        return graphDatabase.findNode(label, GraphStaticKeys.ID, id);
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

package com.tramchester.graph;

import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.graph.Relationships.TransportRelationship;
import org.neo4j.gis.spatial.indexprovider.SpatialIndexProvider;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;

import java.util.LinkedList;
import java.util.List;


public class GraphQuery {

    private RelationshipFactory relationshipFactory;
    private GraphDatabaseService graphDatabaseService;

    public GraphQuery(GraphDatabaseService graphDatabaseService, RelationshipFactory relationshipFactory) {
        this.graphDatabaseService = graphDatabaseService;
        this.relationshipFactory = relationshipFactory;
    }

    public Node getStationNode(String stationId) {
        Node result;
        try (Transaction tx = graphDatabaseService.beginTx()) {
            result = graphDatabaseService.findNode(DynamicLabel.label(TransportGraphBuilder.STATION),
                    GraphStaticKeys.ID, stationId);
            tx.success();
        }
        return result;
    }

    public Node getRouteStationNode(String routeStationId) {
        Node result;
        try (Transaction tx = graphDatabaseService.beginTx()) {
            result = graphDatabaseService.findNode(DynamicLabel.label(TransportGraphBuilder.ROUTE_STATION),
                    GraphStaticKeys.ID, routeStationId);
            tx.success();
        }
        return result;
    }

    public Index<Node> getSpatialIndex() {
        return graphDatabaseService.index().forNodes("spatial_index", SpatialIndexProvider.SIMPLE_POINT_CONFIG);
    }

    public List<TransportRelationship> getRouteStationRelationships(String routeStationId, Direction direction) {
        Node routeStationNode = getRouteStationNode(routeStationId);
        List<TransportRelationship> result = new LinkedList<>();
        try (Transaction tx = graphDatabaseService.beginTx()) {
            routeStationNode.getRelationships(direction).forEach(
                    relationship -> result.add(relationshipFactory.getRelationship(relationship)));
            tx.success();
        }
        return result;
    }

    public List<TransportRelationship> getStationRelationships(String routeStationId, Direction direction) {

        Node stationNode = getStationNode(routeStationId);
        List<TransportRelationship> result = new LinkedList<>();
        try (Transaction tx = graphDatabaseService.beginTx()) {
            stationNode.getRelationships(direction).forEach(relationship -> result.add(
                    relationshipFactory.getRelationship(relationship)));
            tx.success();
        }
        return result;
    }
}

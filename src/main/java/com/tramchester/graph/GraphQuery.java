package com.tramchester.graph;

import com.tramchester.domain.Route;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.graph.Relationships.TransportRelationship;
import org.neo4j.gis.spatial.SimplePointLayer;
import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.gis.spatial.encoders.SimplePointEncoder;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.Traverser;

import java.util.LinkedList;
import java.util.List;


public class GraphQuery {

    private RelationshipFactory relationshipFactory;
    private SpatialDatabaseService spatialDatabaseService;
    private GraphDatabaseService graphDatabaseService;

    public GraphQuery(GraphDatabaseService graphDatabaseService, RelationshipFactory relationshipFactory,
                      SpatialDatabaseService spatialDatabaseService) {
        this.graphDatabaseService = graphDatabaseService;
        this.relationshipFactory = relationshipFactory;
        this.spatialDatabaseService = spatialDatabaseService;
    }

    public Node getStationNode(String stationId) {
        Node result;
        try (Transaction tx = graphDatabaseService.beginTx()) {
            result = graphDatabaseService.findNode(TransportGraphBuilder.Labels.STATION,
                    GraphStaticKeys.ID, stationId);
            tx.success();
        }
        return result;
    }

    public Node getRouteStationNode(String routeStationId) {
        Node result;
        try (Transaction tx = graphDatabaseService.beginTx()) {
            result = graphDatabaseService.findNode(TransportGraphBuilder.Labels.ROUTE_STATION,
                    GraphStaticKeys.ID, routeStationId);
            tx.success();
        }
        return result;
    }

    public ResourceIterable<Node> getAllForRouteNoTx(Route route) {
        Traverser traversal = graphDatabaseService.traversalDescription().
                depthFirst().
                relationships(TransportRelationshipTypes.TRAM_GOES_TO, Direction.OUTGOING).
                evaluator(new RouteEvaluator(route)).traverse();
        return traversal.nodes();
    }

    public SimplePointLayer getSpatialLayer() {
        return (SimplePointLayer) spatialDatabaseService.getOrCreateLayer("stations",
                SimplePointEncoder.class, SimplePointLayer.class);
    }

    public List<TransportRelationship> getRouteStationRelationships(String routeStationId, Direction direction) throws TramchesterException {
        Node routeStationNode = getRouteStationNode(routeStationId);
        if (routeStationNode==null) {
            throw new TramchesterException("Unable to find routeStationNode with ID " + routeStationId);
        }
        List<TransportRelationship> result = new LinkedList<>();
        try (Transaction tx = graphDatabaseService.beginTx()) {
            routeStationNode.getRelationships(direction).forEach(
                    relationship -> result.add(relationshipFactory.getRelationship(relationship)));
            tx.success();
        }
        return result;
    }

}

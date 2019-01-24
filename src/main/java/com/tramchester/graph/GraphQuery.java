package com.tramchester.graph;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.graph.Relationships.TransportRelationship;
import org.neo4j.gis.spatial.SimplePointLayer;
import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.gis.spatial.encoders.SimplePointEncoder;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.Traverser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class GraphQuery {
    private static final Logger logger = LoggerFactory.getLogger(GraphQuery.class);

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

    public Node getPlatformNode(String id) {
        Node result;
        try (Transaction tx = graphDatabaseService.beginTx()) {
            result = graphDatabaseService.findNode(TransportGraphBuilder.Labels.PLATFORM,
                    GraphStaticKeys.ID, id);
            tx.success();
        }
        return result;
    }

    public Node getServiceNode(String id) {
        Node result;
        try (Transaction tx = graphDatabaseService.beginTx()) {
            result = graphDatabaseService.findNode(TransportGraphBuilder.Labels.SERVICE,
                    GraphStaticKeys.ID, id);
            tx.success();
        }
        return result;
    }

    public Node getAreaNode(String areaName) {
        Node result;
        try (Transaction tx = graphDatabaseService.beginTx()) {
            result = graphDatabaseService.findNode(TransportGraphBuilder.Labels.AREA,
                    GraphStaticKeys.ID, areaName);
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

    public ArrayList<Node> findStartNodesFor(String routeName) {
        ArrayList<Node> arrayList = new ArrayList<>();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("routeName",routeName);
        String query = "MATCH (begin:ROUTE_STATION) " +
                "WHERE begin.route_name={ routeName } " +
                "AND NOT ()-[:TRAM_GOES_TO]->(begin) " +
                "RETURN begin";

        Result results = graphDatabaseService.execute(query,parameters);
        ResourceIterator<Node> nodes = results.columnAs("begin");
        nodes.forEachRemaining(n -> arrayList.add(n));
        logger.debug(String.format("Found %s start nodes for route %s", arrayList.size(), routeName));
        if (arrayList.size()>1) {
            logger.warn(String.format("Found more than one (%s) start nodes for route %s", arrayList.size(), routeName));
        }
        return arrayList;
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

package com.tramchester.integration.graph;

import com.tramchester.domain.Route;
import com.tramchester.integration.graph.Relationships.RelationshipFactory;
import org.neo4j.gis.spatial.SimplePointLayer;
import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StationIndexs {
    private static final Logger logger = LoggerFactory.getLogger(StationIndexs.class);

    protected final RelationshipFactory relationshipFactory;
    private Map<String,Node> routeStationNodeCache;
    private Map<String,Node> stationNodeCache;
    private SimplePointLayer spatialLayer;

    protected GraphDatabaseService graphDatabaseService;
    protected GraphQuery graphQuery;
    private boolean warnIfMissing;

    public StationIndexs(GraphDatabaseService graphDatabaseService, RelationshipFactory relationshipFactory,
                         SpatialDatabaseService spatialDatabaseService, boolean warnIfMissing) {
        this.graphDatabaseService = graphDatabaseService;
        graphQuery = new GraphQuery(graphDatabaseService, relationshipFactory, spatialDatabaseService);
        this.warnIfMissing = warnIfMissing;
        this.relationshipFactory = relationshipFactory;
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

    public Stream<Node> getNodesFor(Route route) {
        logger.info("Find nodes for route " + route);
        ResourceIterable<Node> iterable = graphQuery.getAllForRouteNoTx(route.getName());
        return StreamSupport.stream(iterable.spliterator(),false);
    }

    protected SimplePointLayer getSpatialLayer() {
        if (spatialLayer == null) {
            spatialLayer = graphQuery.getSpatialLayer();
        }
        return spatialLayer;
    }

}

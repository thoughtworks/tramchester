package com.tramchester.graph;

import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

public class FindRouteEndPoints {
    private static final Logger logger = LoggerFactory.getLogger(FindRouteEndPoints.class);
    private final GraphDatabase graphDatabase;

    @Inject
    public FindRouteEndPoints(GraphDatabase graphDatabase, StagedTransportGraphBuilder.Ready readyToken) {
        this.graphDatabase = graphDatabase;
    }

    public IdSet<RouteStation> searchForStarts(TransportMode mode) {
        logger.info("Find starts for " +mode);

        String query = "MATCH (a:ROUTE_STATION)-[r1:ON_ROUTE]->(:ROUTE_STATION) " +
                "WHERE $mode in r1.transport_mode " +
                "AND " +
                        "NOT EXISTS { " +
                            "MATCH (:ROUTE_STATION)-[r2:ON_ROUTE]->(a:ROUTE_STATION) " +
                            "WHERE $mode in r2.transport_mode AND r1.route_id=r2.route_id" +
                        "}" +
                " RETURN a";

        IdSet<RouteStation> stationIds = getIdFors(mode, query);

        logger.info("Found " + stationIds.size() + " starts ");
        return stationIds;
    }

    public IdSet<RouteStation> searchForEnds(TransportMode mode) {
        logger.info("Find ends for " +mode);

        String query = "MATCH (:ROUTE_STATION)-[r1:ON_ROUTE]->(a:ROUTE_STATION) " +
                "WHERE $mode in r1.transport_mode " +
                "AND " +
                "NOT EXISTS { " +
                "MATCH (a:ROUTE_STATION)-[r2:ON_ROUTE]->(:ROUTE_STATION) " +
                "WHERE $mode in r2.transport_mode AND r1.route_id=r2.route_id" +
                "}" +
                " RETURN a";

        IdSet<RouteStation> stationIds = getIdFors(mode, query);
        logger.info("Found " + stationIds.size() + " ends");
        return stationIds;
    }

    @NotNull
    private IdSet<RouteStation> getIdFors(TransportMode mode, String query) {
        logger.debug("Query: '" + query + '"');

        Map<String, Object> params = new HashMap<>();
        params.put("mode", mode.getNumber());

        IdSet<RouteStation> stationIds = new IdSet<>();
        try (Transaction txn  = graphDatabase.beginTx()) {
            Result result = txn.execute(query, params);
            while (result.hasNext()) {
                Map<String, Object> row = result.next();
                Node node = (Node) row.get("a");
                stationIds.add(GraphProps.getRouteStationIdFrom(node));
            }
            result.close();
        }
        return stationIds;
    }

}

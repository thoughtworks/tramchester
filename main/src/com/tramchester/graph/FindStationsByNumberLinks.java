package com.tramchester.graph;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.graphbuild.*;
import com.tramchester.metrics.TimedTransaction;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

@LazySingleton
public class FindStationsByNumberLinks {
    private static final Logger logger = LoggerFactory.getLogger(FindStationsByNumberLinks.class);

    private final GraphDatabase graphDatabase;

    @Inject
    public FindStationsByNumberLinks(GraphDatabase graphDatabase, CompositeStationGraphBuilder.Ready readyToken) {
        this.graphDatabase = graphDatabase;
    }

    public IdSet<Station> atLeastNLinkedStations(TransportMode mode, int threshhold) {
        logger.info(format("Find at least N outbound for %s N=%s", mode, threshhold));
        Map<String, Object> params = new HashMap<>();

        String stationLabel = GraphLabel.forMode(mode).name();

        params.put("threshhold", threshhold);
        params.put("mode", mode.getNumber());
        // todo b has to be a STATION
        String query = format("MATCH (a:%s)-[link:LINKED]->(b) " +
                        "WHERE $mode in link.transport_modes " +
                        "WITH a, count(link) as num " +
                        "WHERE num>=$threshhold " +
                        "RETURN a",
                stationLabel);

        return doQuery(mode, params, query);
    }

    @NotNull
    private IdSet<Station> doQuery(TransportMode mode, Map<String, Object> params, String query) {
        logger.info("Query: '" + query + '"');

        IdSet<Station> stationIds = new IdSet<>();

        try (TimedTransaction timedTransaction = new TimedTransaction(graphDatabase, logger, "linked for " + mode) ) {
            Transaction txn = timedTransaction.transaction();
            Result result = txn.execute(query, params);
            while (result.hasNext()) {
                Map<String, Object> row = result.next();
                Node node = (Node) row.get("a");
                stationIds.add(GraphProps.getStationId(node));
            }
            result.close();
        }

        logger.info("Found " + stationIds.size() + " matches");
        return stationIds;
    }

}

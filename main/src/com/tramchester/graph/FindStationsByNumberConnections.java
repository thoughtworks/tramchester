package com.tramchester.graph;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.graphbuild.StationsAndLinksGraphBuilder;
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
public class FindStationsByNumberConnections {
    private static final Logger logger = LoggerFactory.getLogger(FindStationsByNumberConnections.class);

    private final GraphDatabase graphDatabase;

    @Inject
    public FindStationsByNumberConnections(GraphDatabase graphDatabase, StationsAndLinksGraphBuilder.Ready readyToken) {
        this.graphDatabase = graphDatabase;
    }

    public IdSet<Station> findFor(TransportMode mode, int threshhold, boolean exact) {
        logger.info(format("Find for %s threshhold %s", mode, threshhold));
        long start = System.currentTimeMillis();
        Map<String, Object> params = new HashMap<>();
        String stationLabel = GraphBuilder.Labels.forMode(mode).name();
        String modesProps = GraphPropertyKey.TRANSPORT_MODES.getText();
        String predicate = exact ? "=" : ">=";

        params.put("count", threshhold);
        params.put("mode", mode.getNumber());

        String query = format("MATCH (a:%s)-[r:LINKED]->(b) " +
                        "WHERE $mode in r.%s " +
                        "WITH a, count(r) as num " +
                        "WHERE num%s$count " +
                        "RETURN a",
                stationLabel, modesProps, predicate);

        logger.info("Query: '" + query + '"');

        IdSet<Station> stationIds = new IdSet<>();
        try (Transaction txn  = graphDatabase.beginTx()) {
            Result result = txn.execute(query, params);
            while (result.hasNext()) {
                Map<String, Object> row = result.next();
                Node node = (Node) row.get("a");
                stationIds.add(GraphProps.getStationId(node));
            }
        }
        long duration = System.currentTimeMillis()-start;
        logger.info("Took " + duration);
        return stationIds;
    }
}

package com.tramchester.graph;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.graph.search.PopulateNodeIdsFromQuery;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.*;


@LazySingleton
public class HourNodeCache extends PopulateNodeIdsFromQuery {
    private static final Logger logger = LoggerFactory.getLogger(HourNodeCache.class);
    private static final int HOURS = 24;

    private final ArrayList<Set<Long>> forEachHour;

    @Inject
    public HourNodeCache(GraphDatabase graphDatabaseService, StagedTransportGraphBuilder.Ready ready) {
        super(graphDatabaseService);
        forEachHour = new ArrayList<>(HOURS);
    }

    @PostConstruct
    public void start() {
        logger.info("Starting");
        for (int hour = 0; hour < HOURS; hour++) {
            forEachHour.add(getNodeIdsFor(hour));
            logger.info("Added " + forEachHour.get(hour).size() + " nodes for hour " + hour);
        }
        logger.info("Started");
    }

    @PreDestroy
    public void stop() {
        logger.info("Stopping");
        for (int hour = 0; hour < HOURS; hour++) {
            forEachHour.get(hour).clear();
        }
        forEachHour.clear();
        logger.info("Stopped");
    }

    /***
     * public for test support
     */
    public Set<Long> getNodeIdsFor(int hour) {

        Map<String, Object> params = new HashMap<>();
        params.put("hour", hour);
        String query = "MATCH (node:HOUR) " +
                "WHERE node.hour=$hour " +
                "RETURN ID(node) as id";

        return getNodeIdsForQuery(params, query);
    }

    public int getHourFor(long relationshipId) {
        for (int hour = 0; hour < HOURS; hour++) {
            if (forEachHour.get(hour).contains(relationshipId)) {
                return hour;
            }
        }
        throw new RuntimeException("Missing relationshipId " + relationshipId);
    }
}

package com.tramchester.graph;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.graph.search.PopulateNodeIdsFromQuery;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@LazySingleton
public class HourNodeCache extends PopulateNodeIdsFromQuery {
    private static final Logger logger = LoggerFactory.getLogger(HourNodeCache.class);
    private static final int HOURS = 24;

    private final ArrayList<Set<Long>> forEachHour;

    @Inject
    public HourNodeCache(GraphDatabase graphDatabase, StagedTransportGraphBuilder.Ready ready) {
        super(graphDatabase);
        forEachHour = new ArrayList<>(HOURS);
    }

    @PostConstruct
    public void start() {
        logger.info("Starting");
        ConcurrentMap<Integer, Set<Long>> results = IntStream.rangeClosed(0, 23).parallel().
                mapToObj(hour -> Pair.of(hour, getNodeIdsFor(hour))).
                collect(Collectors.toConcurrentMap(Pair::getLeft, Pair::getRight));

        for (int hour = 0; hour < HOURS; hour++) {
            forEachHour.add(results.get(hour));
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

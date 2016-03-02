package com.tramchester;

import com.codahale.metrics.health.HealthCheck;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphHealthCheck extends HealthCheck {
    private static final Logger logger = LoggerFactory.getLogger(GraphHealthCheck.class);

    private static final long TIMEOUT_MILLIS = 5;
    private final GraphDatabaseService service;

    public GraphHealthCheck(GraphDatabaseService service) {
        this.service = service;
    }

    @Override
    protected Result check() throws Exception {
        if (service.isAvailable(TIMEOUT_MILLIS)) {
            logger.info("Graph DB available");
            return Result.healthy();
        }
        logger.warn("Graph DB unavailable");
        return Result.unhealthy("Graph DB unavailable");
    }
}

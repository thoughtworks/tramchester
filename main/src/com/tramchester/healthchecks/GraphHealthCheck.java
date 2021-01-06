package com.tramchester.healthchecks;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.graph.GraphDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

@LazySingleton
public class GraphHealthCheck extends TramchesterHealthCheck {
    private static final Logger logger = LoggerFactory.getLogger(GraphHealthCheck.class);
    private static final String unavailable = "Graph DB unavailable";

    private static final long TIMEOUT_MILLIS = 5;
    private final GraphDatabase service;

    @Inject
    public GraphHealthCheck(GraphDatabase service) {
        this.service = service;
    }

    @Override
    protected Result check() {
        if (service.isAvailable(TIMEOUT_MILLIS)) {
            logger.info("Graph DB available");
            return Result.healthy();
        }
        logger.error(unavailable);
        return Result.unhealthy(unavailable);
    }

    @Override
    public String getName() {
        return "graphDB";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}

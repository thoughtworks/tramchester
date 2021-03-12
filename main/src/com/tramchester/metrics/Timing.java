package com.tramchester.metrics;

import java.time.Duration;
import java.time.Instant;

public class Timing implements AutoCloseable {
    private final String name;
    private final Instant start;
    private final org.slf4j.Logger logger;

    public Timing(org.slf4j.Logger logger, String name) {
        this.name = name;
        this.logger = logger;
        this.start = Instant.now();

        logger.info("Start " + name);
    }

    @Override
    public void close() {
        Instant finish = Instant.now();
        logger.info("TIMING: " + name + " TOOK: " + Duration.between(start, finish).toMillis() +" ms");
    }
}

package com.tramchester.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledFuture;

public class LiveDataJobHealthCheck extends HealthCheck {
    private static final Logger logger = LoggerFactory.getLogger(LiveDataJobHealthCheck.class);

    private final ScheduledFuture<?> liveDataFuture;

    public LiveDataJobHealthCheck(ScheduledFuture<?> liveDataFuture) {
        this.liveDataFuture = liveDataFuture;
    }

    @Override
    protected Result check() {
        logger.info("Live data job check");
        if (liveDataFuture.isDone()) {
            logger.error("Live data job is done");
            return Result.unhealthy("Live data job is done");
        } else if (liveDataFuture.isCancelled()) {
            logger.error("Live data job is cancelled");
            return Result.unhealthy("Live data job is cancelled");
        } else return Result.healthy();
    }
}

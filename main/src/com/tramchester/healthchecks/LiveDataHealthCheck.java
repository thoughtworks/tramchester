package com.tramchester.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.tramchester.domain.TramTime;
import com.tramchester.repository.LiveDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

public class LiveDataHealthCheck extends HealthCheck {
    private static final Logger logger = LoggerFactory.getLogger(LiveDataHealthCheck.class);

    private LiveDataRepository repository;
    private final ProvidesNow providesNow;
    private final String noEntriesPresent = "no entries present";

    public LiveDataHealthCheck(LiveDataRepository repository, ProvidesNow providesNow) {
        this.repository = repository;
        this.providesNow = providesNow;
    }

    @Override
    public Result check() {
        logger.info("Checking live data health");
        int total = repository.countEntries();

        if (total==0) {
            logger.error(noEntriesPresent);
            return Result.unhealthy(noEntriesPresent);
        }

        long stale = repository.staleDataCount();
        if (stale!=0L) {
            String message = format("%s of %s entries are stale", stale, total);
            logger.error(message);
            return Result.unhealthy(message);
        }

        TramTime queryTime = providesNow.getNow();

        long notExpired = repository.upToDateEntries(queryTime);
        if (notExpired!=total) {
            String message = format("%s of %s entries are expired at %s", total-notExpired, total, queryTime);
            logger.error(message);
            return Result.unhealthy(message);
        }

        String msg = format("Live data healthy with %s entires", total);
        logger.info(msg);
        return Result.healthy(msg);
    }
}

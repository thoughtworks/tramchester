package com.tramchester.healthchecks;

import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.repository.LiveDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

public class LiveDataHealthCheck extends TramchesterHealthCheck {
    private static final Logger logger = LoggerFactory.getLogger(LiveDataHealthCheck.class);

    private final LiveDataRepository repository;
    private final ProvidesNow providesNow;
    private static final String noEntriesPresent = "no entries present";

    public LiveDataHealthCheck(LiveDataRepository repository, ProvidesNow providesNow) {
        this.repository = repository;
        this.providesNow = providesNow;
    }

    @Override
    public Result check() {
        logger.info("Checking live data health");
        int total = repository.upToDateEntries();

        if (total==0) {
            logger.error(noEntriesPresent);
            return Result.unhealthy(noEntriesPresent);
        }

        long stale = repository.missingDataCount();
        if (stale!=0L) {
            String message = format("%s of %s entries are stale", stale, total);
            logger.error(message);
            return Result.unhealthy(message);
        }

        TramTime queryTime = providesNow.getNow();

        long notExpired = repository.upToDateEntries();
        if (notExpired!=total) {
            String message = format("%s of %s entries are expired at %s", total-notExpired, total, queryTime);
            logger.error(message);
            return Result.unhealthy(message);
        }

        String msg = format("Live data healthy with %s entires", total);
        logger.info(msg);
        return Result.healthy(msg);
    }

    @Override
    public String getName() {
        return "liveData";
    }
}

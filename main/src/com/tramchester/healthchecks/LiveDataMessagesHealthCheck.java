package com.tramchester.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.tramchester.domain.TramTime;
import com.tramchester.repository.LiveDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

public class LiveDataMessagesHealthCheck extends HealthCheck {
    private static final Logger logger = LoggerFactory.getLogger(LiveDataMessagesHealthCheck.class);

    private LiveDataRepository repository;
    private final String noEntriesPresent = "no messages present";

    public LiveDataMessagesHealthCheck(LiveDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public Result check() {
        logger.info("Checking live data health");
        int total = repository.countMessages();

        if (total<1) {
            logger.error(noEntriesPresent);
            return Result.unhealthy(noEntriesPresent);
        }

        String msg = format("Live data messages healthy with %s entires", total);
        logger.info(msg);
        return Result.healthy(msg);
    }
}

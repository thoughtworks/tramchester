package com.tramchester.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.tramchester.repository.LiveDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

public class LiveDataMessagesHealthCheck extends HealthCheck {
    private static final Logger logger = LoggerFactory.getLogger(LiveDataMessagesHealthCheck.class);

    private LiveDataRepository repository;

    public LiveDataMessagesHealthCheck(LiveDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public Result check() {
        logger.info("Checking live data health");
        int entries = repository.countEntries();
        int messages = repository.countMessages();

        // Note: Zero case will trigger LiveDataHealthCheck
        if (messages<entries) {
            String message = format("Not enough messages present, %s out of %s entries", messages, entries);
            logger.warn(message);
            return Result.unhealthy(message);
        }

        String msg = format("Live data messages healthy with %s entries", messages);
        logger.info(msg);
        return Result.healthy(msg);
    }
}

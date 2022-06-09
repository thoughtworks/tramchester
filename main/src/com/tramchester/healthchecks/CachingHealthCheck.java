package com.tramchester.healthchecks;

import com.tramchester.domain.time.ProvidesNow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;

public class CachingHealthCheck extends TramchesterHealthCheck {
    private static final Logger logger = LoggerFactory.getLogger(CachingHealthCheck.class);

    private final TramchesterHealthCheck containedCheck;
    private final Duration cacheDuration;
    private final ProvidesNow providesLocalNow;

    private LocalDateTime lastCheck;
    private Result result;

    public CachingHealthCheck(TramchesterHealthCheck containedCheck, Duration cacheDuration, ProvidesNow providesNow) {
        super(containedCheck.getServiceTimeLimits());

        this.containedCheck = containedCheck;
        this.cacheDuration = cacheDuration;
        this.providesLocalNow = providesNow;
    }

    @Override
    public String getName() {
        return containedCheck.getName();
    }

    @Override
    public boolean isEnabled() {
        return containedCheck.isEnabled();
    }

    @Override
    protected Result check() throws Exception {
        return execute();
    }

    @Override
    public Result execute()  {
        LocalDateTime currentTime = providesLocalNow.getDateTime();

        if  (lastCheck!=null) {
            Duration since = Duration.between(lastCheck, currentTime);
            if (since.compareTo(cacheDuration)<0) {
                logger.info("Using cached result for " + containedCheck.getName());
               return result;
            }
        }
        result = containedCheck.execute();
        lastCheck = currentTime;

        return result;
    }
}

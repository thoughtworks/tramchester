package com.tramchester.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.tramchester.domain.ServiceTimeLimits;

import java.time.LocalDateTime;

public abstract class TramchesterHealthCheck extends HealthCheck {

    private final ServiceTimeLimits serviceTimeLimits;

    protected TramchesterHealthCheck(ServiceTimeLimits serviceTimeLimits) {
        this.serviceTimeLimits = serviceTimeLimits;
    }

    public abstract String getName();

    protected boolean isLateNight(LocalDateTime dateTime) {
        return !serviceTimeLimits.within(dateTime.toLocalTime());
    }

    public abstract boolean isEnabled();
}

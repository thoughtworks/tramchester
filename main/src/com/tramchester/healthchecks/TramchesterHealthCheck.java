package com.tramchester.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.tramchester.domain.ServiceTimeLimits;
import com.tramchester.domain.time.TramTime;

import java.time.LocalDateTime;
import java.time.LocalTime;

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

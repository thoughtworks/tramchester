package com.tramchester.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.tramchester.domain.time.TramTime;

import java.time.LocalDateTime;

public abstract class TramchesterHealthCheck extends HealthCheck {
    private final TramTime startOfNight = TramTime.of(2,0);
    private final TramTime endOfNight = TramTime.of(6, 10);

    public abstract String getName();

    protected boolean isLateNight(LocalDateTime dateTime) {
        return TramTime.of(dateTime).between(startOfNight, endOfNight);
    }

    public abstract boolean isEnabled();
}

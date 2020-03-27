package com.tramchester.healthchecks;

import com.codahale.metrics.health.HealthCheck;

public abstract class TramchesterHealthCheck extends HealthCheck {
    public abstract String getName();
}

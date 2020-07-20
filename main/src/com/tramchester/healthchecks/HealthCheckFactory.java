package com.tramchester.healthchecks;

import java.util.Collection;

public interface HealthCheckFactory {
    Collection<TramchesterHealthCheck> getHealthChecks();
}

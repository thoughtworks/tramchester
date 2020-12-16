package com.tramchester.healthchecks;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.netflix.governator.guice.lazy.LazySingleton;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

@LazySingleton
public class RegistersHealthchecks {

    private final Set<TramchesterHealthCheck> healthChecks;

    @Inject
    public RegistersHealthchecks(DataExpiryHealthCheckFactory dataExpiryHealthCheckFactory,
                                 NewDataAvailableHealthCheckFactory newDataAvailableHealthCheckFactory,
                                 GraphHealthCheck graphHealthCheck,
                                 LiveDataHealthCheck liveDataHealthCheck,
                                 LiveDataMessagesHealthCheck liveDataMessagesHealthCheck,
                                 LiveDataS3UploadHealthCheck liveDataS3UploadHealthCheck) {
        this.healthChecks = new HashSet<>();
        healthChecks.addAll(dataExpiryHealthCheckFactory.getHealthChecks());
        healthChecks.addAll(newDataAvailableHealthCheckFactory.getHealthChecks());
        healthChecks.add(graphHealthCheck);
        healthChecks.add(liveDataHealthCheck);
        healthChecks.add(liveDataMessagesHealthCheck);
        healthChecks.add(liveDataS3UploadHealthCheck);
    }

    public void registerAllInto(HealthCheckRegistry registry) {
        healthChecks.forEach(healthCheck -> registry.register(healthCheck.getName(), healthCheck));
    }
}

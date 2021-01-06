package com.tramchester.healthchecks;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.netflix.governator.guice.lazy.LazySingleton;

import javax.inject.Inject;
import java.util.Collection;
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

        addIfEnabled(dataExpiryHealthCheckFactory.getHealthChecks());
        addIfEnabled(newDataAvailableHealthCheckFactory.getHealthChecks());
        addIfEnabled(graphHealthCheck);

        // live data healthchecks
        addIfEnabled(liveDataHealthCheck);
        addIfEnabled(liveDataMessagesHealthCheck);
        addIfEnabled(liveDataS3UploadHealthCheck);
    }


    private void addIfEnabled(Collection<TramchesterHealthCheck> healthChecks) {
        healthChecks.forEach(this::addIfEnabled);
    }

    private void addIfEnabled(TramchesterHealthCheck healthCheck) {
        if (healthCheck.isEnabled()) {
            healthChecks.add(healthCheck);
        }
    }

    public void registerAllInto(HealthCheckRegistry registry) {
        healthChecks.forEach(healthCheck -> registry.register(healthCheck.getName(), healthCheck));
    }
}

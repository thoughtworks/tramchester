package com.tramchester.healthchecks;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.repository.TransportData;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@LazySingleton
public class DataExpiryHealthCheckFactory implements HealthCheckFactory {
    private final List<TramchesterHealthCheck> healthChecks;
    private final TransportData transportData;
    private final ProvidesLocalNow providesLocalNow;
    private final TramchesterConfig config;

    @Inject
    public DataExpiryHealthCheckFactory(TransportData transportData, ProvidesLocalNow providesLocalNow, TramchesterConfig config) {
        this.transportData = transportData;
        this.providesLocalNow = providesLocalNow;
        this.config = config;
        healthChecks = new ArrayList<>();
    }

    @Override
    public Collection<TramchesterHealthCheck> getHealthChecks() {
        return healthChecks;
    }

    @PreDestroy
    public void dispose() {
        healthChecks.clear();
    }

    @PostConstruct
    public void start() {
        transportData.getFeedInfos().forEach((name, feedInfo) -> {
            TramchesterHealthCheck healthCheck = new DataExpiryHealthCheck(feedInfo, name, providesLocalNow, config);
            healthChecks.add(healthCheck);
        });
    }

}

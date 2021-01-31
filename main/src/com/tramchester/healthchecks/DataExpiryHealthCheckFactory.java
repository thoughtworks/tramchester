package com.tramchester.healthchecks;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.ServiceTimeLimits;
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
    private final ServiceTimeLimits serviceTimeLimits;

    @Inject
    public DataExpiryHealthCheckFactory(TransportData transportData, ProvidesLocalNow providesLocalNow,
                                        TramchesterConfig config, ServiceTimeLimits serviceTimeLimits) {
        this.transportData = transportData;
        this.providesLocalNow = providesLocalNow;
        this.config = config;
        this.serviceTimeLimits = serviceTimeLimits;
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
            TramchesterHealthCheck healthCheck = new DataExpiryHealthCheck(feedInfo, name, providesLocalNow, config, serviceTimeLimits);
            healthChecks.add(healthCheck);
        });
    }

}

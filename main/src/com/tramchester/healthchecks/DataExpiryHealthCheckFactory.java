package com.tramchester.healthchecks;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.repository.TransportData;
import org.picocontainer.Disposable;
import org.picocontainer.Startable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@LazySingleton
public class DataExpiryHealthCheckFactory implements HealthCheckFactory, Startable, Disposable {
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
    @Override
    public void dispose() {
        healthChecks.clear();
    }

    @PostConstruct
    @Override
    public void start() {
        transportData.getFeedInfos().forEach((name, feedInfo) -> {
            TramchesterHealthCheck healthCheck = new DataExpiryHealthCheck(feedInfo, name, providesLocalNow, config);
            healthChecks.add(healthCheck);
        });
    }

    @Override
    public void stop() {
        // no-op
    }
}

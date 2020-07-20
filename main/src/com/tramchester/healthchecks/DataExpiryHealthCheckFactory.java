package com.tramchester.healthchecks;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.repository.TransportData;
import org.picocontainer.Disposable;
import org.picocontainer.Startable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DataExpiryHealthCheckFactory implements HealthCheckFactory, Startable, Disposable {
    private final List<TramchesterHealthCheck> healthChecks;
    private final TransportData transportData;
    private final ProvidesLocalNow providesLocalNow;
    private final TramchesterConfig config;

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

    @Override
    public void dispose() {
        healthChecks.clear();
    }

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

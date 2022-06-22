package com.tramchester.modules;

import com.google.inject.AbstractModule;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.graph.caches.CachedNodeOperations;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.metrics.CacheMetrics;

public class MappersAndConfigurationModule extends AbstractModule {
    private final TramchesterConfig config;
    private final CacheMetrics.RegistersCacheMetrics registersCacheMetrics;

    public MappersAndConfigurationModule(TramchesterConfig config, CacheMetrics.RegistersCacheMetrics registersCacheMetrics) {
        this.config = config;
        this.registersCacheMetrics = registersCacheMetrics;
    }

    @Override
    protected void configure() {
        bind(TramchesterConfig.class).toInstance(config);
        bind(CacheMetrics.RegistersCacheMetrics.class).toInstance(registersCacheMetrics);

        bind(ProvidesNow.class).to(ProvidesLocalNow.class);
        bind(NodeContentsRepository.class).to(CachedNodeOperations.class);
    }

}

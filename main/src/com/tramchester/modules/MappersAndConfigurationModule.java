package com.tramchester.modules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.graph.caches.*;
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

    @SuppressWarnings("unused")
    @LazySingleton
    @Provides
    CsvMapper providesCsvMapper() {
        return  CsvMapper.builder().addModule(new AfterburnerModule()).build();
    }

    @SuppressWarnings("unused")
    @LazySingleton
    @Provides
    ObjectMapper providesObjectMapper() {
        return new ObjectMapper();
    }

}

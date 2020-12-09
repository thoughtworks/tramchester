package com.tramchester;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.healthchecks.TramchesterHealthCheck;
import com.tramchester.repository.ReportsCacheStats;
import com.tramchester.repository.TransportData;
import com.tramchester.resources.APIResource;

import java.util.List;

public interface ComponentContainer {
    // load data from files, see below for version that can be used for testing injecting alternative TransportDataSource
    void initialise(TramchesterConfig configuration);

    // init dependencies but possibly with alternative source of transport data
    void initialise(TramchesterConfig configuration, TransportData transportData);

    <T> T get(Class<T> klass);

    List<APIResource> getResources();

    void close();

    List<TramchesterHealthCheck> getHealthChecks();

    List<ReportsCacheStats> getHasCacheStat();
}

package com.tramchester.repository;

import com.tramchester.config.DataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.reference.TransportMode;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class TransportModeRepository {
    private final TramchesterConfig config;

    @Inject
    public TransportModeRepository(TramchesterConfig config) {
        this.config = config;
    }

    public Set<TransportMode> getModes() {
        return config.getDataSourceConfig().stream().
                map(DataSourceConfig::getTransportModes).
                flatMap(Collection::stream).
                map(TransportMode::fromGTFS).collect(Collectors.toSet());
    }
}

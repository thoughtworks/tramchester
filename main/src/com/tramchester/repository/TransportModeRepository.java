package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.reference.TransportMode;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@LazySingleton
public class TransportModeRepository {
    private final TramchesterConfig config;

    @Inject
    public TransportModeRepository(TramchesterConfig config) {
        this.config = config;
    }

    public Set<TransportMode> getModes() {
        return config.getGTFSDataSource().stream().
                map(GTFSSourceConfig::getTransportModes).
                flatMap(Collection::stream).
                collect(Collectors.toSet());
    }
}

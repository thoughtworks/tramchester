package com.tramchester.integration.testSupport.rail;

import com.tramchester.config.RailConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.reference.TransportMode;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.tramchester.domain.reference.TransportMode.RailReplacementBus;
import static com.tramchester.domain.reference.TransportMode.Train;

public class TestRailConfig implements RailConfig {
    private final RailRemoteDataSourceConfig remoteConfig;

    public TestRailConfig(RailRemoteDataSourceConfig remoteConfig) {
        this.remoteConfig = remoteConfig;
    }

    @Override
        public boolean getOnlyMarkedInterchanges() {
            return true;
        }

        @Override
        public DataSourceID getDataSourceId() {
            return DataSourceID.rail;
        }

        @Override
        public Path getDataPath() {
            return Path.of("data/rail");
        }

        @Override
        public Path getStations() {
            return Path.of(remoteConfig.getFilePrefix() + ".msn");
        }

        @Override
        public Path getTimetable() {
            return Path.of(remoteConfig.getFilePrefix() + ".mca");
        }

    @Override
    public Set<TransportMode> getModes() {
        return new HashSet<>(Arrays.asList(Train, RailReplacementBus));
    }

    @Override
    public String getVersion() {
        return RailRemoteDataSourceConfig.VERSION;
    }

}

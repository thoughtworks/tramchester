package com.tramchester.integration.testSupport.train;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import org.glassfish.jersey.internal.util.JerseyPublisher;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.tramchester.domain.reference.GTFSTransportationType.*;

public class RailTestDataSourceConfig implements GTFSSourceConfig {
    private final String dataPath;

    public RailTestDataSourceConfig(String dataPath) {
        this.dataPath = dataPath;
    }

    // https://planar.network/projects/feeds

    @Override
    public Path getDataPath() {
        return Paths.get(dataPath);
    }

    @Override
    public String getName() {
        return "gbRail";
    }

    @Override
    public boolean getHasFeedInfo() {
        return false;
    }

    @Override
    public Set<GTFSTransportationType> getTransportGTFSModes() {
        return new HashSet<>(Arrays.asList(subway, ferry, train, bus, replacementBus));
    }

    @Override
    public Set<TransportMode> getTransportModesWithPlatforms() {
        return Collections.emptySet();
    }

    @Override
    public Set<LocalDate> getNoServices() {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getAdditionalInterchanges() {
        return Collections.emptySet();
    }

    @Override
    public Set<TransportMode> compositeStationModes() {
        return Collections.emptySet();
    }
}

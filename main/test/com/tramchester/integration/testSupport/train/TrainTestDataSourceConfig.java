package com.tramchester.integration.testSupport.train;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.domain.StationClosure;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;

import static com.tramchester.domain.reference.GTFSTransportationType.*;

public class TrainTestDataSourceConfig implements GTFSSourceConfig {
    private final String dataPath;

    public TrainTestDataSourceConfig(String dataPath) {
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

    @Override
    public List<StationClosure> getStationClosures() {
        return Collections.emptyList();
    }

    @Override
    public boolean getAddWalksForClosed() {
        return false;
    }
}

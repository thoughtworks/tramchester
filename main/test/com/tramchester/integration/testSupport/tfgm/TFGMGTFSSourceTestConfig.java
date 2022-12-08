package com.tramchester.integration.testSupport.tfgm;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.domain.StationClosures;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class TFGMGTFSSourceTestConfig implements GTFSSourceConfig {

    //
    // https://data.gov.uk/dataset/c3ca6469-7955-4a57-8bfc-58ef2361b797/gm-public-transport-schedules-new-gtfs-dataset
    //

    private final String dataFolder;
    private final Set<GTFSTransportationType> sourceModes;
    private final Set<TransportMode> modesWithPlatforms;
    private final IdSet<Station> additionalInterchanges;
    private final Set<TransportMode> compositeStationModes;
    private final List<StationClosures> closedStations;
    private final Duration maxInitialWait;

    public TFGMGTFSSourceTestConfig(String dataFolder, Set<GTFSTransportationType> sourceModes,
                                    Set<TransportMode> modesWithPlatforms, IdSet<Station> additionalInterchanges,
                                    Set<TransportMode> compositeStationModes, List<StationClosures> closedStations, Duration maxInitialWait) {
        this.dataFolder = dataFolder;
        this.sourceModes = sourceModes;
        this.modesWithPlatforms = modesWithPlatforms;
        this.additionalInterchanges = additionalInterchanges;
        this.compositeStationModes = compositeStationModes;
        this.closedStations = closedStations;
        this.maxInitialWait = maxInitialWait;
    }

    public TFGMGTFSSourceTestConfig(String dataFolder, GTFSTransportationType mode, TransportMode modeWithPlatform,
                                    IdSet<Station> additionalInterchanges, Set<TransportMode> groupStationModes,
                                    List<StationClosures> closedStations, Duration maxInitialWait) {
        this(dataFolder, Collections.singleton(mode), Collections.singleton(modeWithPlatform),
                additionalInterchanges, groupStationModes, closedStations, maxInitialWait);
    }

    @Override
    public Path getDataPath() {
        return Paths.get(dataFolder);
    }

    @Override
    public String getName() {
        return "tfgm";
    }

    @Override
    public boolean getHasFeedInfo() {
        return true;
    }

    @Override
    public Set<GTFSTransportationType> getTransportGTFSModes() {
        return sourceModes;
    }

    @Override
    public Set<TransportMode> getTransportModesWithPlatforms() {
        return modesWithPlatforms;
    }

    @Override
    public Set<LocalDate> getNoServices() {
        return Collections.emptySet();
    }

    @Override
    public IdSet<Station> getAdditionalInterchanges() {
        return additionalInterchanges;
    }

    @Override
    public Set<TransportMode> compositeStationModes() {
        return compositeStationModes;
    }

    @Override
    public List<StationClosures> getStationClosures() {
        return closedStations;
    }

    @Override
    public boolean getAddWalksForClosed() {
        return true;
    }

    @Override
    public boolean getOnlyMarkedInterchanges() {
        return false;
    }

    @Override
    public Duration getMaxInitialWait() {
        return maxInitialWait;
    }
}

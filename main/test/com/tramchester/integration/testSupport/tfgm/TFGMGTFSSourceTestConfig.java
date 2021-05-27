package com.tramchester.integration.testSupport.tfgm;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;

public class TFGMGTFSSourceTestConfig implements GTFSSourceConfig {

    //
    // https://data.gov.uk/dataset/c96c4140-8b6c-4130-9642-49866498d268/gm-public-transport-schedules-gtfs
    //

    private final String dataFolder;
    private final Set<GTFSTransportationType> sourceModes;
    private final Set<TransportMode> modesWithPlatforms;
    private final Set<String> additionalInterchanges;

    public TFGMGTFSSourceTestConfig(String dataFolder, Set<GTFSTransportationType> sourceModes, Set<TransportMode> modesWithPlatforms, Set<String> additionalInterchanges) {
        this.dataFolder = dataFolder;
        this.sourceModes = sourceModes;
        this.modesWithPlatforms = modesWithPlatforms;
        this.additionalInterchanges = additionalInterchanges;
    }

    public TFGMGTFSSourceTestConfig(String dataFolder, GTFSTransportationType mode, TransportMode modeWithPlatform, Set<String> additionalInterchanges) {
        this(dataFolder, Collections.singleton(mode), Collections.singleton(modeWithPlatform), additionalInterchanges);
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
    public Set<String> getAdditionalInterchanges() {
        return additionalInterchanges;
    }
}

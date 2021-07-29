package com.tramchester.integration.testSupport;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.testSupport.AdditionalTramInterchanges;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.integration.testSupport.tfgm.TFGMGTFSSourceTestConfig;
import com.tramchester.testSupport.TestEnv;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.tramchester.domain.reference.TransportMode.Bus;
import static com.tramchester.domain.reference.TransportMode.Tram;

public class NeighboursTestConfig extends IntegrationBusTestConfig {

    public NeighboursTestConfig() {
        super("neighboursTest", "createNeighboursTest.db");
    }

    @Override
    protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
        final Set<TransportMode> modesWithPlatforms = Collections.singleton(Tram);
        final Set<TransportMode> compositeStationModes = Collections.singleton(Bus);
        return Collections.singletonList(
                new TFGMGTFSSourceTestConfig("data/bus", TestEnv.tramAndBus,
                        modesWithPlatforms, AdditionalTramInterchanges.get(), compositeStationModes, Collections.emptyList()));
    }

    @Override
    public boolean getCreateNeighbours() {
        return true;
    }

    @Override
    public Path getCacheFolder() {
        return TestEnv.CACHE_DIR.resolve("neighboursIntegration");
    }
}

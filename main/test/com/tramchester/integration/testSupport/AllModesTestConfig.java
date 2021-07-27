package com.tramchester.integration.testSupport;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.BoundingBox;
import com.tramchester.integration.testSupport.tfgm.TFGMGTFSSourceTestConfig;
import com.tramchester.integration.testSupport.tfgm.TFGMRemoteDataSourceConfig;
import com.tramchester.integration.testSupport.train.RailRemoteDataSourceConfig;
import com.tramchester.integration.testSupport.train.RailTestDataSourceConfig;
import com.tramchester.testSupport.AdditionalTramInterchanges;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.tramchester.domain.reference.TransportMode.Bus;
import static com.tramchester.domain.reference.TransportMode.Tram;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
public class AllModesTestConfig extends IntegrationTestConfig {

    private TFGMRemoteDataSourceConfig remoteTfgmSourceConfig;
    private RailRemoteDataSourceConfig remoteDataRailConfig;

    public AllModesTestConfig() {
        super(new DBConfig("allModesTest", "allModesTest.db"));
    }

    @Override
    protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
        final Set<TransportMode> modesWithPlatforms = Collections.singleton(Tram);
        final Set<TransportMode> compositeStationModes = Collections.singleton(Bus);

        final TFGMGTFSSourceTestConfig tfgmDataSource = new TFGMGTFSSourceTestConfig("data/bus", TestEnv.tramAndBus,
                modesWithPlatforms, AdditionalTramInterchanges.get(), compositeStationModes);
        RailTestDataSourceConfig railSourceConfig = new RailTestDataSourceConfig("data/trains");

        remoteTfgmSourceConfig = new TFGMRemoteDataSourceConfig("data/bus");
        remoteDataRailConfig = new RailRemoteDataSourceConfig("data/trains");

        return Arrays.asList(tfgmDataSource, railSourceConfig);
    }

    @Override
    public BoundingBox getBounds() {
        return TestEnv.getTFGMBusBounds();
    }

    @Override
    public List<RemoteDataSourceConfig> getRemoteDataSourceConfig() {
        return Arrays.asList(remoteDataRailConfig, remoteNaptanConfig, remoteTfgmSourceConfig);
    }

    @Override
    public boolean getChangeAtInterchangeOnly() {
        return true;
    }


    @Override
    public boolean getCreateNeighbours() {
        return true;
    }

    @Override
    public Path getCacheFolder() {
        return TestEnv.CACHE_DIR.resolve("allModes");
    }

    private static class DBConfig extends GraphDBTestConfig {

        public DBConfig(String folder, String dbName) {
            super(folder, dbName);
        }

        @Override
        public String getNeo4jPagecacheMemory() {
            return "300m";
        }

    }
}

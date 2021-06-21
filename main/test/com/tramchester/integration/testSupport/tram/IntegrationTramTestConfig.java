package com.tramchester.integration.testSupport.tram;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.LiveDataConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.testSupport.AdditionalTramInterchanges;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.GraphDBTestConfig;
import com.tramchester.integration.testSupport.IntegrationTestConfig;
import com.tramchester.integration.testSupport.tfgm.TFGMGTFSSourceTestConfig;
import com.tramchester.integration.testSupport.tfgm.TFGMRemoteDataSourceConfig;
import com.tramchester.testSupport.TestLiveDataConfig;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class IntegrationTramTestConfig extends IntegrationTestConfig {

    private static final String DB_NAME = "int_test_tram.db";

    private final GTFSSourceConfig gtfsSourceConfig;
    protected final RemoteDataSourceConfig remoteTFGMConfig;
    private final boolean liveDataEnabled;

    public IntegrationTramTestConfig() {
       this(DB_NAME, false);
    }

    public IntegrationTramTestConfig(boolean liveDataEnabled) {
        this(DB_NAME, liveDataEnabled);
    }

    protected IntegrationTramTestConfig(String dbName) {
        this(dbName, false);
    }

    private IntegrationTramTestConfig(String dbName, boolean liveDataEnabled) {
        super(new GraphDBIntegrationTramTestConfig("integrationTramTest", dbName));
        this.liveDataEnabled = liveDataEnabled;
        gtfsSourceConfig = new TFGMGTFSSourceTestConfig("data/tram", GTFSTransportationType.tram,
                TransportMode.Tram, AdditionalTramInterchanges.get(), Collections.emptySet());
        remoteTFGMConfig = new TFGMRemoteDataSourceConfig("data/tram");
    }

    @Override
    protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
        return Collections.singletonList(gtfsSourceConfig);
    }

    @Override
    public List<RemoteDataSourceConfig> getRemoteDataSourceConfig() {
        return Collections.singletonList(remoteTFGMConfig); // naptan disabled for trams
    }

    @Override
    public int getNumberQueries() { return 1; }

    @Override
    public int getQueryInterval() {
        return 6;
    }

    @Override
    public LiveDataConfig getLiveDataConfig() {
        if (liveDataEnabled) {
            return new TestLiveDataConfig();
        }
        return null;
    }

    private static class GraphDBIntegrationTramTestConfig extends GraphDBTestConfig {
        public GraphDBIntegrationTramTestConfig(String folder, String dbFilename) {
            super(folder, dbFilename);
        }

        @Override
        public String getNeo4jPagecacheMemory() {
            return "100m";
        }
    }

    @Override
    public Path getCacheFolder() {
        return Path.of("testData/cache/tramIntegration");
    }
}


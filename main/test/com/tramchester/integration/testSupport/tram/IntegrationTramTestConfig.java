package com.tramchester.integration.testSupport.tram;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.LiveDataConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.domain.StationClosure;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.GraphDBTestConfig;
import com.tramchester.integration.testSupport.IntegrationTestConfig;
import com.tramchester.integration.testSupport.tfgm.TFGMGTFSSourceTestConfig;
import com.tramchester.integration.testSupport.tfgm.TFGMRemoteDataSourceConfig;
import com.tramchester.testSupport.AdditionalTramInterchanges;
import com.tramchester.testSupport.TestEnv;
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
       this(DB_NAME, false, Collections.emptyList());
    }

    public IntegrationTramTestConfig(boolean liveDataEnabled) {
        this(DB_NAME, liveDataEnabled, Collections.emptyList());
    }

    public IntegrationTramTestConfig(String dbName, List<StationClosure> closedStations) {
        this(dbName, false, closedStations);
    }

    protected IntegrationTramTestConfig(String dbName) {
        this(dbName, false, Collections.emptyList());
    }

    private IntegrationTramTestConfig(String dbName, boolean liveDataEnabled, List<StationClosure> closedStations) {
        this(new GraphDBIntegrationTramTestConfig("integrationTramTest", dbName), liveDataEnabled, closedStations);
    }

    protected IntegrationTramTestConfig(GraphDBTestConfig dbTestConfig, boolean liveDataEnabled, List<StationClosure> closedStations) {
        super(dbTestConfig);
        this.liveDataEnabled = liveDataEnabled;
        gtfsSourceConfig = new TFGMGTFSSourceTestConfig("data/tram", GTFSTransportationType.tram,
                TransportMode.Tram, AdditionalTramInterchanges.stations(), Collections.emptySet(), closedStations);
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
        return TestEnv.CACHE_DIR.resolve("tramIntegration");
    }
}


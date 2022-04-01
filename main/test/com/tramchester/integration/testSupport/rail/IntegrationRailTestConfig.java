package com.tramchester.integration.testSupport.rail;

import com.tramchester.config.*;
import com.tramchester.geo.BoundingBox;
import com.tramchester.integration.testSupport.GraphDBTestConfig;
import com.tramchester.integration.testSupport.IntegrationTestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TestOpenLdbConfig;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class IntegrationRailTestConfig extends IntegrationTestConfig {

    private final GraphDBConfig graphDBConfig;

    public IntegrationRailTestConfig() {
        this("rail_tramchester.db");
    }

    protected IntegrationRailTestConfig(String dbFilename) {
        super(new GraphDBIntegrationRailTestConfig("integrationTrainTest", dbFilename));
        graphDBConfig = new GraphDBIntegrationRailTestConfig("integrationTrainTest", dbFilename);

    }

    @Override
    public RailConfig getRailConfig() {
       return new TestRailConfig(railRemoteDataSource);
    }

    @Override
    public BoundingBox getBounds() {
        return TestEnv.getTrainBounds();
    }

    @Override
    protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
        return Collections.emptyList();
    }

    @Override
    public List<RemoteDataSourceConfig> getRemoteDataSourceConfig() {
        return List.of(railRemoteDataSource, remoteNaptanXMLConfig, remoteNPTGconfig);
    }

    @Override
    public boolean getChangeAtInterchangeOnly() {
        return true;
    }

    @Override
    public GraphDBConfig getGraphDBConfig() {
        return graphDBConfig;
    }

    @Override
    public int getMaxJourneyDuration() {
        return 820;
    } // 13.5 hours Aberdeen to Penzance is ~810 minutes

    @Override
    public int getMaxInitialWait() {
        return 25;
    }

    @Override
    public int getMaxWait() {
        return 30;
    }

    @Override
    public int getNumberQueries() { return 3; }

    @Override
    public int getQueryInterval() { return 20; }

    @Override
    public Path getCacheFolder() {
        return TestEnv.CACHE_DIR.resolve("railIntegration");
    }

    private static class GraphDBIntegrationRailTestConfig extends GraphDBTestConfig {

        public GraphDBIntegrationRailTestConfig(String folder, String dbName) {
            super(folder, dbName);
        }

        @Override
        public String getNeo4jPagecacheMemory() {
            return "1000m";
        }
    }

    @Override
    public OpenLdbConfig getOpenldbwsConfig() {
        return new TestOpenLdbConfig();
    }
}

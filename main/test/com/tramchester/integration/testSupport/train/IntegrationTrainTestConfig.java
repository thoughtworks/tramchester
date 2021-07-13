package com.tramchester.integration.testSupport.train;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.GraphDBConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.geo.BoundingBox;
import com.tramchester.integration.testSupport.GraphDBTestConfig;
import com.tramchester.integration.testSupport.IntegrationTestConfig;
import com.tramchester.testSupport.TestEnv;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class IntegrationTrainTestConfig extends IntegrationTestConfig {

    private final RailTestDataSourceConfig sourceConfig;
    private final RemoteDataSourceConfig remoteDataRailConfig;
    private final GraphDBConfig graphDBConfig;

    public IntegrationTrainTestConfig() {
        this("train_tramchester.db");
    }

    protected IntegrationTrainTestConfig(String dbFilename) {
        super(new GraphDBIntegrationTrainTestConfig("integrationTrainTest", dbFilename));
        graphDBConfig = new GraphDBIntegrationTrainTestConfig("integrationTrainTest", dbFilename);
        sourceConfig = new RailTestDataSourceConfig("data/trains");
        remoteDataRailConfig = new RailRemoteDataSourceConfig("data/trains");

    }

    @Override
    public BoundingBox getBounds() {
        return TestEnv.getTrainBounds();
    }

    @Override
    protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
        return Collections.singletonList(sourceConfig);
    }

    @Override
    public List<RemoteDataSourceConfig> getRemoteDataSourceConfig() {
        return Arrays.asList(remoteDataRailConfig, remoteNaptanConfig);
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
        return 840;
    }

    @Override
    public int getMaxInitialWait() {
        return 25;
    }

    @Override
    public int getMaxWait() {
        return 35;
    }

    @Override
    public int getNumberQueries() { return 6; }

    @Override
    public int getQueryInterval() { return 10; }

    @Override
    public Path getCacheFolder() {
        return TestEnv.CACHE_DIR.resolve("trainIntegration");
    }

    private static class GraphDBIntegrationTrainTestConfig extends GraphDBTestConfig {

        public GraphDBIntegrationTrainTestConfig(String folder, String dbName) {
            super(folder, dbName);
        }

        @Override
        public String getNeo4jPagecacheMemory() {
            return "1000m";
        }
    }
}

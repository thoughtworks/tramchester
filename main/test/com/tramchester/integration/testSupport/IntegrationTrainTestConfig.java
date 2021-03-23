package com.tramchester.integration.testSupport;

import com.tramchester.config.DataSourceConfig;
import com.tramchester.config.GraphDBConfig;
import com.tramchester.geo.BoundingBox;
import com.tramchester.testSupport.TestEnv;

import java.util.Collections;
import java.util.List;

public class IntegrationTrainTestConfig extends IntegrationTestConfig {

    private final RailTestDataSourceConfig sourceConfig;
    private final GraphDBConfig graphDBConfig;

    public IntegrationTrainTestConfig() {
        this("train_tramchester.db");
    }

    protected IntegrationTrainTestConfig(String dbFilename) {
        super(new GraphDBIntegrationTrainTestConfig("integrationTrainTest", dbFilename));
        graphDBConfig = new GraphDBIntegrationTrainTestConfig("integrationTrainTest", dbFilename);
        sourceConfig = new RailTestDataSourceConfig();
    }

    @Override
    public BoundingBox getBounds() {
        return TestEnv.getTrainBounds();
    }

    @Override
    protected List<DataSourceConfig> getDataSourceFORTESTING() {
        return Collections.singletonList(sourceConfig);
    }

    @Override
    public boolean getChangeAtInterchangeOnly() {
        return false;
    }

    @Override
    public GraphDBConfig getGraphDBConfig() {
        return graphDBConfig;
    }

    @Override
    public int getMaxJourneyDuration() {
        return 8*60;
    }

    @Override
    public int getMaxInitialWait() {
        return 15;
    }

    @Override
    public int getMaxWait() {
        return 60;
    }

    @Override
    public int getNumberQueries() { return 4; }

    @Override
    public int getQueryInterval() { return 15; }

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

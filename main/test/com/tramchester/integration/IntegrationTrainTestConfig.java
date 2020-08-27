package com.tramchester.integration;

import com.tramchester.config.DataSourceConfig;
import com.tramchester.geo.BoundingBox;
import com.tramchester.testSupport.TestEnv;

import java.util.Collections;
import java.util.List;

public class IntegrationTrainTestConfig extends IntegrationTestConfig {

    private final RailTestDataSourceConfig sourceConfig;

    public IntegrationTrainTestConfig() {
        this("train_tramchester.db");
    }

    private IntegrationTrainTestConfig(String dbName) {
        super("integrationTrainTest", dbName);
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
    public String getNeo4jPagecacheMemory() {
        // TODO
        throw new RuntimeException("TODO Define me");
    }
}

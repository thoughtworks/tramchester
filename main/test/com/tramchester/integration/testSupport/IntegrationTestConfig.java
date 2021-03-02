package com.tramchester.integration.testSupport;

import com.tramchester.config.GraphDBConfig;
import com.tramchester.testSupport.TestConfig;

import java.nio.file.Path;

public abstract class IntegrationTestConfig extends TestConfig {
    private final GraphDBTestConfig dbConfig;

    protected IntegrationTestConfig(GraphDBTestConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    /**
     * Use version that takes GraphDBTestConfig
     */
    @Deprecated
    public IntegrationTestConfig(String folder, String dbName) {
        this(new GraphDBTestConfig(folder, dbName));
    }

    public Path getDBPath() {
        return dbConfig.getDbPath();
    }

    @Override
    public GraphDBConfig getGraphDBConfig() {
        return dbConfig;
    }
}

package com.tramchester.integration.testSupport;

import com.tramchester.config.GraphDBConfig;
import com.tramchester.integration.testSupport.naptan.NaptanRemoteDataSourceConfig;
import com.tramchester.testSupport.TestConfig;

import java.nio.file.Path;

public abstract class IntegrationTestConfig extends TestConfig {

    protected final NaptanRemoteDataSourceConfig remoteNaptanConfig;
    private final GraphDBTestConfig dbConfig;

    protected IntegrationTestConfig(GraphDBTestConfig dbConfig) {
        remoteNaptanConfig = new NaptanRemoteDataSourceConfig("data/naptan");
        this.dbConfig = dbConfig;
    }

    @Override
    public GraphDBConfig getGraphDBConfig() {
        return dbConfig;
    }
}

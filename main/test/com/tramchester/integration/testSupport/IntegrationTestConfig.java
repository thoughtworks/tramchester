package com.tramchester.integration.testSupport;

import com.tramchester.config.GraphDBConfig;
import com.tramchester.integration.testSupport.naptan.NaptanRemoteDataSourceConfig;
import com.tramchester.testSupport.TestConfig;

public abstract class IntegrationTestConfig extends TestConfig {

    protected final NaptanRemoteDataSourceConfig remoteNaptanConfig;
    protected final PostCodeDatasourceConfig postCodeDatasourceConfig;
    private final GraphDBTestConfig dbConfig;

    protected IntegrationTestConfig(GraphDBTestConfig dbConfig) {
        remoteNaptanConfig = new NaptanRemoteDataSourceConfig("data/naptan");
        postCodeDatasourceConfig = new PostCodeDatasourceConfig();
        this.dbConfig = dbConfig;
    }

    @Override
    public GraphDBConfig getGraphDBConfig() {
        return dbConfig;
    }
}

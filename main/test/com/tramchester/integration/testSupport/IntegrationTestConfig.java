package com.tramchester.integration.testSupport;

import com.tramchester.config.GraphDBConfig;
import com.tramchester.integration.testSupport.naptan.NaptanRemoteDataSourceConfig;
import com.tramchester.testSupport.TestConfig;

import java.nio.file.Path;

public abstract class IntegrationTestConfig extends TestConfig {

    protected final NaptanRemoteDataSourceConfig remoteNaptanCSVConfig;
    protected final NaptanRemoteDataSourceConfig remoteNaptanXMLConfig;
    protected final PostCodeDatasourceConfig postCodeDatasourceConfig;
    private final GraphDBTestConfig dbConfig;

    protected IntegrationTestConfig(GraphDBTestConfig dbConfig) {
        final Path naptanLocalDataPath = Path.of("data/naptan");
        remoteNaptanCSVConfig = new NaptanRemoteDataSourceConfig(naptanLocalDataPath, false);
        remoteNaptanXMLConfig = new NaptanRemoteDataSourceConfig(naptanLocalDataPath, true);

        postCodeDatasourceConfig = new PostCodeDatasourceConfig();
        this.dbConfig = dbConfig;
    }

    @Override
    public GraphDBConfig getGraphDBConfig() {
        return dbConfig;
    }
}

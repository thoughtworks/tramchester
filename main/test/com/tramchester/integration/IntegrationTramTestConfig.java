package com.tramchester.integration;

import com.tramchester.config.DataSourceConfig;
import com.tramchester.domain.GTFSTransportationType;
import com.tramchester.testSupport.TestConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

public class IntegrationTramTestConfig extends TestConfig {

    private final Path dbPath;
    private final boolean exists;
    private final DataSourceConfig dataSourceConfig;

    public IntegrationTramTestConfig() {
       this("int_test_tramchester.db");
    }

    public IntegrationTramTestConfig(String dbName) {
        this.dbPath = Path.of("databases", "integrationTramTest", dbName);
        exists = Files.exists(dbPath);
        dataSourceConfig = new TFGMTestDataSourceConfig("data/tram", Collections.singleton(GTFSTransportationType.tram));
    }

    @Override
    final public String getGraphName() {
        return dbPath.toAbsolutePath().toString();
    }

    @Override
    protected DataSourceConfig getTestDataSourceConfig() {
        return dataSourceConfig;
    }

    @Override
    public boolean getRebuildGraph() {
        return !exists;
    }

    @Override
    public int getNumberQueries() { return 1; }

    @Override
    public int getQueryInterval() {
        return 6;
    }

    public Path getDBPath() {
        return dbPath;
    }

}


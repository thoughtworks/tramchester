package com.tramchester.integration.testSupport;

import com.tramchester.testSupport.TestConfig;

import java.nio.file.Path;

public abstract class IntegrationTestConfig extends TestConfig {
    private final Path dbPath;

    public IntegrationTestConfig(String folder, String dbName) {
        this.dbPath = Path.of("databases", folder, dbName);
    }

    @Override
    public String getGraphName() {
        return dbPath.toAbsolutePath().toString();
    }

    public Path getDBPath() {
        return dbPath;
    }

}

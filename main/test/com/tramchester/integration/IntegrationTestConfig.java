package com.tramchester.integration;

import com.tramchester.testSupport.TestConfig;

import java.nio.file.Files;
import java.nio.file.Path;

public abstract class IntegrationTestConfig extends TestConfig {
    private final Path dbPath;
    private final boolean exists;

    public IntegrationTestConfig(String folder, String dbName) {
        this.dbPath = Path.of("databases", folder, dbName);
        exists = Files.exists(dbPath);
    }

    @Override
    public boolean getRebuildGraph() {
        return !exists;
    }

    @Override
    public String getGraphName() {
        return dbPath.toAbsolutePath().toString();
    }

    public Path getDBPath() {
        return dbPath;
    }

}

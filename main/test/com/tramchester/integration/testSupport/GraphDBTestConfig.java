package com.tramchester.integration.testSupport;

import com.tramchester.config.GraphDBConfig;

import java.nio.file.Path;

public class GraphDBTestConfig implements GraphDBConfig {
    private final Path dbPath;

    public GraphDBTestConfig(String folder, String dbName) {
        this.dbPath = Path.of("databases", folder, dbName);
    }

    @Override
    public String getGraphName() {
        return dbPath.toString();
    }

    public Path getDbPath() {
        return dbPath;
    }

    @Override
    public String getNeo4jPagecacheMemory() {
        return "100m";
    }
}

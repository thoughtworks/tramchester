package com.tramchester.integration.testSupport;

import com.tramchester.config.GraphDBConfig;

import java.nio.file.Path;

public class GraphDBTestConfig implements GraphDBConfig {
    private final Path containingFolder;
    private final Path dbPath;

    public GraphDBTestConfig(String folder, String dbName) {
        this.containingFolder = Path.of("databases", folder);
        this.dbPath = containingFolder.resolve(dbName);
    }

    @Override
    public String getGraphName() {
        return dbPath.toString();
    }

    public Path getDbPath() {
        return dbPath;
    }

    public Path getContainingFolder() {
        return containingFolder;
    }

    @Override
    public String getNeo4jPagecacheMemory() {
        return "100m";
    }
}

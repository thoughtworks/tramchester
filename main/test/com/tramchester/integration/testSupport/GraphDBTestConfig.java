package com.tramchester.integration.testSupport;

import com.tramchester.config.GraphDBConfig;

import java.nio.file.Path;

public class GraphDBTestConfig implements GraphDBConfig {
    private final Path containingFolder;
    private final Path fullpath;

    public GraphDBTestConfig(String subFolderForDB, String dbFilename) {
        this.containingFolder = Path.of("databases", subFolderForDB);
        this.fullpath = containingFolder.resolve(dbFilename);
    }

    public Path getDbPath() {
        return fullpath;
    }

    public Path getContainingFolder() {
        return containingFolder;
    }

    @Override
    public String getNeo4jPagecacheMemory() {
        return "100m";
    }
}

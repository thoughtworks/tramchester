package com.tramchester.integration.testSupport;

import com.tramchester.config.GraphDBConfig;

import java.nio.file.Path;

public class GraphDBTestConfig implements GraphDBConfig {
    private final Path fullpath;

    public GraphDBTestConfig(String subFolderForDB, String dbFilename) {
        Path containingFolder = Path.of("databases", subFolderForDB);
        this.fullpath = containingFolder.resolve(dbFilename);
    }

    public Path getDbPath() {
        return fullpath;
    }

    @Override
    public String getNeo4jPagecacheMemory() {
        return "100m";
    }

    @Override
    public String getInitialHeapSize() {
        return "100m";
    }

    @Override
    public String getMaxHeapSize() {
        return "200m";
    }
}

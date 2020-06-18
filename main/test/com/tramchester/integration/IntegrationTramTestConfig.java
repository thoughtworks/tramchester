package com.tramchester.integration;

import com.tramchester.testSupport.TestConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class IntegrationTramTestConfig extends TestConfig {

    private final Path dbPath;
    private final boolean exists;

    public IntegrationTramTestConfig() {
       this("int_test_tramchester.db");
    }

    public IntegrationTramTestConfig(String dbName) {
        this.dbPath = Path.of("databases", "integrationTramTest", dbName);
        exists = Files.exists(dbPath);
    }

    @Override
    final public String getGraphName() {
        return dbPath.toAbsolutePath().toString();
    }

    @Override
    public boolean getRebuildGraph() {
        return !exists;
    }

    @Override
    public Set<String> getAgencies() {
        return new HashSet<>(Collections.singletonList("MET"));
    }

    @Override
    public boolean getCreateLocality() {
        return false;
    }

    @Override
    public Path getDataFolder() {
        return Paths.get("data/tram");
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


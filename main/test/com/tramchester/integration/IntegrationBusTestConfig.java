package com.tramchester.integration;

import com.tramchester.testSupport.TestConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class IntegrationBusTestConfig extends TestConfig {
    private final Path dbPath;
    private final boolean exists;

    public IntegrationBusTestConfig() {
        this("bus_tramchester.db");
    }

    private IntegrationBusTestConfig(String dbName) {
        this.dbPath = Path.of("databases", "integrationBusTest", dbName);
        exists = Files.exists(dbPath);
    }

    @Override
    public boolean getBus() {
        return true;
    }

    @Override
    public Set<String> getAgencies() {
            //return new HashSet<>(Arrays.asList("MET","GMS"));
        // Empty set means all
        return Collections.emptySet();
    }

    @Override
    public boolean getChangeAtInterchangeOnly() {
        return false;
    }

    @Override
    public Path getDataFolder() {
        return Paths.get("data/bus");
    }

    @Override
    public boolean getRebuildGraph() {
        return !exists;
    }

    @Override
    public String getGraphName() {
        return dbPath.toAbsolutePath().toString();
    }

    @Override
    public int getNumberQueries() { return 1; }

    @Override
    public int getQueryInterval() {
        return 6;
    }

}

package com.tramchester.integration;

import com.tramchester.domain.GTFSTransportationType;
import com.tramchester.testSupport.TestConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class IntegrationTrainTestConfig extends TestConfig {
    private final Path dbPath;
    private final boolean exists;

    public IntegrationTrainTestConfig() {
        this("train_tramchester.db");
    }

    private IntegrationTrainTestConfig(String dbName) {
        this.dbPath = Path.of("databases", "integrationTrainTest", dbName);
        exists = Files.exists(dbPath);
    }

    @Override
    public List<GTFSTransportationType> getTransportModes() {
        return Arrays.asList(GTFSTransportationType.train);
    }

    @Override
    public boolean getChangeAtInterchangeOnly() {
        return false;
    }

    @Override
    public Path getDataFolder() {
        return Paths.get("data/gb-rail-latest");
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
    public int getNumberQueries() { return 3; }

    @Override
    public int getQueryInterval() {
        return 12;
    }

    @Override
    public double getDistanceToNeighboursKM() {
        return 0.4;
    }
}

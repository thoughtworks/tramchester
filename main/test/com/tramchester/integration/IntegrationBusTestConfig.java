package com.tramchester.integration;

import com.tramchester.config.DataSourceConfig;
import com.tramchester.domain.GTFSTransportationType;
import com.tramchester.testSupport.TestConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class IntegrationBusTestConfig extends TestConfig {
    private final Path dbPath;
    private final boolean exists;
    private final DataSourceConfig dataSourceConfig;

    public IntegrationBusTestConfig() {
        this("bus_tramchester.db");
    }

    private IntegrationBusTestConfig(String dbName) {
        this.dbPath = Path.of("databases", "integrationBusTest", dbName);
        exists = Files.exists(dbPath);
        dataSourceConfig = new TFGMTestDataSourceConfig("data/bus");
    }

    @Override
    public List<GTFSTransportationType> getTransportModes() {
        return Arrays.asList(GTFSTransportationType.tram,GTFSTransportationType.bus);
    }

    @Override
    protected DataSourceConfig getTestDataSourceConfig() {
        return dataSourceConfig;
    }

    @Override
    public boolean getChangeAtInterchangeOnly() {
        return false;
    }

//    @Override
//    public Path getDataFolder() {
//        return Paths.get("data/bus");
//    }

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

package com.tramchester.integration;

import com.tramchester.testSupport.TestConfig;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.server.ServerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class IntegrationBusTestConfig extends TestConfig {
    private final Path dbName;

    public IntegrationBusTestConfig() {
        this("bus_tramchester.db");
    }

    public IntegrationBusTestConfig(String dbName) {
        this.dbName = Path.of("databases", "integrationBusTest", dbName);
    }

    @Override
    public boolean getBus() {
        return true;
    }

    @Override
    public Set<String> getAgencies() {
            return new HashSet<>(Arrays.asList("MET","GMS"));
        // Empty set means all
//        return Collections.emptySet();
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
        return !Files.exists(dbName);
    }

    @Override
    public String getGraphName() {
        return dbName.toAbsolutePath().toString();
    }

    @Override
    public int getNumberQueries() { return 1; }

    @Override
    public int getQueryInterval() {
        return 6;
    }

    @Override
    public ServerFactory getServerFactory() {
        DefaultServerFactory factory = new DefaultServerFactory();
        factory.setApplicationContextPath("/");
        factory.setAdminContextPath("/admin");
        factory.setJerseyRootPath("/api/*");
        return factory;
    }
}

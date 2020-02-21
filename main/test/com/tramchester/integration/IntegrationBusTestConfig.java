package com.tramchester.integration;

import com.tramchester.TestConfig;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.server.ServerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class IntegrationBusTestConfig extends TestConfig {
    private final String dbName;

    public IntegrationBusTestConfig(String dbName) {
        this.dbName = dbName;
    }

    @Override
    public Set<String> getAgencies() {
        return new HashSet<>(Arrays.asList("MET","GMS"));
//        return new HashSet<>(Arrays.asList("*"));
    }

    @Override
    public boolean getCreateLocality() {
        return false;
    }

    @Override
    public Path getDataFolder() {
        return Paths.get("data/bus");
    }

    @Override
    public boolean getRebuildGraph() {
        return !Files.exists(Paths.get(dbName));
    }

    @Override
    public String getGraphName() {
        return dbName;
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

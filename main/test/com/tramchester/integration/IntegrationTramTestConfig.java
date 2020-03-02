package com.tramchester.integration;

import com.tramchester.testSupport.TestConfig;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.server.ServerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class IntegrationTramTestConfig extends TestConfig {

    private final Path dbName;

    public IntegrationTramTestConfig() {
       this("int_test_tramchester.db");
    }

    public IntegrationTramTestConfig(String dbName) {
        this.dbName = Path.of("databases", "integrationTramTest", dbName);
    }

    @Override
    final public String getGraphName() {
        return dbName.toAbsolutePath().toString();
    }

    @Override
    public Set<String> getAgencies() {
        return new HashSet<>(Arrays.asList("MET"));
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

    @Override
    public ServerFactory getServerFactory() {
        DefaultServerFactory factory = new DefaultServerFactory();
        factory.setApplicationContextPath("/");
        factory.setAdminContextPath("/admin");
        factory.setJerseyRootPath("/api/*");
        return factory;
    }

}


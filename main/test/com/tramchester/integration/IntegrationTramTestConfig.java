package com.tramchester.integration;

import com.tramchester.TestConfig;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.server.ServerFactory;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class IntegrationTramTestConfig extends TestConfig {

    @Override
    public String getGraphName() {
        return "int_test_tramchester.db";
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

    // just getPlatformById one query time by doing this
    @Override
    public int getQueryInterval() { return getMaxWait()+1; }

    @Override
    public ServerFactory getServerFactory() {
        DefaultServerFactory factory = new DefaultServerFactory();
        factory.setApplicationContextPath("/");
        factory.setAdminContextPath("/admin");
        factory.setJerseyRootPath("/api/*");
        return factory;
    }

}


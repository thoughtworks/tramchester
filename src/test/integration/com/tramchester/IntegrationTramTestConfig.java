package com.tramchester;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class IntegrationTramTestConfig extends TestConfig {

    public IntegrationTramTestConfig() {
    }

    @Override
    public String getGraphName() {
        return "int_test_tramchester.db";
    }

    @Override
    public Set<String> getAgencies() {
        return new HashSet<>(Arrays.asList("MET"));
    }

    @Override
    public boolean isCreateLocality() {
        return false;
    }

    @Override
    public boolean useGenericMapper() {
        return false;
    }

    @Override
    public Path getDataFolder() {
        return Paths.get("data/tram");
    }
}


package com.tramchester;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class IntegrationBusTestConfig extends TestConfig {

    @Override
    public String getGraphName() {
        return "int_test_bus_tramchester.db";
    }

    @Override
    public Set<String> getAgencies() {
        return new HashSet<>(Arrays.asList("*"));
    }

    @Override
    public boolean useGenericMapper() {
        return true;
    }

    @Override
    public Path getDataFolder() {
        return Paths.get("data/bus");
    }
}

package com.tramchester;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class IntegrationTestConfig extends TestConfig {

    public static final String GRAPH_NAME = "int_test_tramchester.db";
    private Set<String> agencies;

    public IntegrationTestConfig() {
        this.agencies = new HashSet<>(Arrays.asList("MET"));
    }

    private boolean graphExists() {
        return new File(GRAPH_NAME).exists();
    }

    @Override
    public boolean isRebuildGraph() {
         return !graphExists();
    }

    @Override
    public boolean isFilterData() {
        return !graphExists();
    }

    @Override
    public String getGraphName() {
        return GRAPH_NAME;
    }

    @Override
    public Set<String> getAgencies() {
        return agencies;
    }

    @Override
    public Path getInputDataPath() {
        return Paths.get("data/tram");
    }

    @Override
    public Path getOutputDataPath() {
        return Paths.get("data/tram");
    }

}


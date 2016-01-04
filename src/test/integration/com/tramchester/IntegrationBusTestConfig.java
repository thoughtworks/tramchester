package com.tramchester;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class IntegrationBusTestConfig extends TestConfig {

    public static final String GRAPH_NAME = "int_test_bus_tramchester.db";
    private Set<String> agencies;
    private Path path = Paths.get("data/bus");

    public IntegrationBusTestConfig() {
        this.agencies = new HashSet<>(Arrays.asList("MET","GMS","GMN"));
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
        return !path.resolve("feed_info.txt").toFile().exists() ;
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
        return path;
    }

    @Override
    public Path getOutputDataPath() {
        return path;
    }

}

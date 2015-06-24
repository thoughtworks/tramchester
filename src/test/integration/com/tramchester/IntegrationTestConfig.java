package com.tramchester;

import com.tramchester.config.TramchesterConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class IntegrationTestConfig extends TramchesterConfig {

    public static final String GRAPH_NAME = "int_test_tramchester.db";
    private boolean rebuildNeeded = false;
    private List<String> closedStations = new ArrayList<>();

    private boolean graphExists() {
        return new File(GRAPH_NAME).exists();
    }

    @Override
    public boolean isRebuildGraph() {
        return rebuildNeeded;
    }

    @Override
    public boolean isPullData() {
        if (graphExists()) {
            rebuildNeeded = false;
            return false;
        }
        rebuildNeeded = true;
        return true;
    }

    @Override
    public String getGraphName() {
        return GRAPH_NAME;
    }

    @Override
    public List<String> getClosedStations() {
        return closedStations;
    }
}


package com.tramchester;

import com.tramchester.config.TramchesterConfig;

import java.io.File;

public class IntegrationTestConfig extends TramchesterConfig {

    public static final String GRAPH_NAME = "int_test_tramchester.db";
    private boolean rebuildNeeded = false;

    private boolean graphExists() {
        return new File(GRAPH_NAME).exists();
    }

    @Override
    public boolean isRebuildGraph() {
        return rebuildNeeded;
//        return true;
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
}


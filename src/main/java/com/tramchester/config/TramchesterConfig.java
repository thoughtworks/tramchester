package com.tramchester.config;

import io.dropwizard.Configuration;

import java.util.List;

public abstract class TramchesterConfig extends Configuration {
    public abstract boolean isRebuildGraph();
    public abstract boolean isPullData();
    public abstract String getGraphName();
    public abstract List<String> getClosedStations();
    public abstract String getInstanceDataBaseURL();

}

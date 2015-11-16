package com.tramchester.config;

import io.dropwizard.Configuration;

import java.util.List;

public abstract class TramchesterConfig extends Configuration {
    public abstract boolean isRebuildGraph();
    public abstract boolean isPullData();
    public abstract boolean isFilterData();
    public abstract String getGraphName();
    public abstract List<String> getClosedStations();
    public abstract List<String> getAgencies();
    public abstract String getInstanceDataBaseURL();
    public abstract String getTramDataUrl();

}

package com.tramchester.config;

import io.dropwizard.Configuration;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public abstract class TramchesterConfig extends Configuration {
    public abstract boolean isRebuildGraph();
    public abstract boolean isPullData();
    public abstract boolean isFilterData();
    public abstract String getGraphName();
    public abstract List<String> getClosedStations();
    public abstract Set<String> getAgencies();
    public abstract String getInstanceDataBaseURL();
    public abstract String getTramDataUrl();

    public abstract Path getInputDataPath();
    public abstract Path getOutputDataPath();

    public abstract boolean useGenericMapper();
}

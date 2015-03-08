package com.tramchester.config;

import io.dropwizard.Configuration;

public abstract class TramchesterConfig extends Configuration {
    public abstract boolean isRebuildGraph();
    public abstract boolean isPullData();
}

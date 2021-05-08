package com.tramchester.config;

import com.google.inject.ImplementedBy;

@ImplementedBy(TramchesterConfig.class)
public interface HasGraphDBConfig {
    GraphDBConfig getGraphDBConfig();
}

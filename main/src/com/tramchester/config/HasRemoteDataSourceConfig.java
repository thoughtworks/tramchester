package com.tramchester.config;

import com.google.inject.ImplementedBy;

import java.util.List;

@ImplementedBy(TramchesterConfig.class)
public interface HasRemoteDataSourceConfig {
    List<RemoteDataSourceConfig> getRemoteDataSourceConfig();
}

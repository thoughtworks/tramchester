package com.tramchester.config;

import com.tramchester.domain.DataSourceID;

import java.time.Duration;

public interface TransportDataSourceConfig {
    boolean getOnlyMarkedInterchanges();

    DataSourceID getDataSourceId();

    Duration getMaxInitialWait();
}

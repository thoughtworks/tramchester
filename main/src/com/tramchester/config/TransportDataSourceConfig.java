package com.tramchester.config;

import com.tramchester.domain.DataSourceID;

public interface TransportDataSourceConfig {
    boolean getOnlyMarkedInterchanges();

    DataSourceID getDataSourceId();
}

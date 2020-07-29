package com.tramchester.integration;

import com.tramchester.config.DataSourceConfig;
import com.tramchester.domain.GTFSTransportationType;

import java.util.*;

public class IntegrationBusTestConfig extends IntegrationTestConfig {
    private final DataSourceConfig dataSourceConfig;

    public IntegrationBusTestConfig() {
        this("bus_tramchester.db");
    }

    private IntegrationBusTestConfig(String dbName) {
        super("integrationBusTest", dbName);
        dataSourceConfig = new TFGMTestDataSourceConfig("data/bus", Collections.singleton(GTFSTransportationType.bus));
    }

    @Override
    protected List<DataSourceConfig> getDataSourceFORTESTING() {
        return Collections.singletonList(dataSourceConfig);
    }

    @Override
    public boolean getChangeAtInterchangeOnly() {
        return false;
    }

    @Override
    public int getNumberQueries() { return 1; }

    @Override
    public boolean getCreateNeighbours() {
        return true;
    }
}

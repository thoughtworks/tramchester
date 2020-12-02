package com.tramchester.integration;

import com.tramchester.config.DataSourceConfig;
import com.tramchester.domain.reference.GTFSTransportationType;

import java.util.*;

public class IntegrationBusTestConfig extends IntegrationTestConfig {
    private final DataSourceConfig dataSourceConfig;

    public IntegrationBusTestConfig() {
        this("bus_tramchester.db");
    }

    public IntegrationBusTestConfig(String dbName) {
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

    @Override
    public int getMaxWait() {
        return 45;
    }

    @Override
    public int getNumOfNearestStopsForWalking() {
        return 50;
    }

    @Override
    public Double getNearestStopRangeKM() {
        return 1.0D;
    }

    @Override
    public Double getNearestStopForWalkingRangeKM() {
        return 0.5D;
    }

    @Override
    public int getMaxJourneyDuration() {
        return 180;
    }

    @Override
    public String getNeo4jPagecacheMemory() {
        return "1300m";
    }
}

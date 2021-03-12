package com.tramchester.integration.testSupport;

import com.tramchester.config.DataSourceConfig;
import com.tramchester.domain.reference.GTFSTransportationType;

import java.util.Collections;
import java.util.List;

public class IntegrationBusTestConfig extends IntegrationTestConfig {
    private final DataSourceConfig dataSourceConfig;

    public IntegrationBusTestConfig() {
        this("int_test_bus.db");
    }

    public IntegrationBusTestConfig(String dbName) {
        super(new GraphDBIntegrationBusTestConfig("integrationBusTest", dbName));
        dataSourceConfig = new TFGMTestDataSourceConfig("data/bus", Collections.singleton(GTFSTransportationType.bus),
                Collections.emptySet());
    }

    @Override
    protected List<DataSourceConfig> getDataSourceFORTESTING() {
        return Collections.singletonList(dataSourceConfig);
    }

    @Override
    public boolean getChangeAtInterchangeOnly() {
        return true;
    }

    @Override
    public int getNumberQueries() { return 1; }

    @Override
    public boolean getCreateNeighbours() {
        return false;
    }

    @Override
    public int getMaxWait() {
        return 25;
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

    private static class GraphDBIntegrationBusTestConfig extends GraphDBTestConfig {

        public GraphDBIntegrationBusTestConfig(String folder, String dbName) {
            super(folder, dbName);
        }

        @Override
        public String getNeo4jPagecacheMemory() {
            return "300m";
        }

    }
}

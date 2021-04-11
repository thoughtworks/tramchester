package com.tramchester.integration.testSupport.bus;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.integration.testSupport.GraphDBTestConfig;
import com.tramchester.integration.testSupport.IntegrationTestConfig;
import com.tramchester.integration.testSupport.naptan.NaptanRemoteDataSourceConfig;
import com.tramchester.integration.testSupport.tfgm.TFGMGTFSSourceTestConfig;
import com.tramchester.integration.testSupport.tfgm.TFGMRemoteDataSourceConfig;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class IntegrationBusTestConfig extends IntegrationTestConfig {
    private final GTFSSourceConfig gtfsSourceConfig;
    private final RemoteDataSourceConfig remoteDataSourceConfig;

    public IntegrationBusTestConfig() {
        this("int_test_bus.db");
    }

    public IntegrationBusTestConfig(String dbName) {
        super(new GraphDBIntegrationBusTestConfig("integrationBusTest", dbName));
        gtfsSourceConfig = new TFGMGTFSSourceTestConfig("data/bus",
                Collections.singleton(GTFSTransportationType.bus),
                Collections.emptySet());
        remoteDataSourceConfig = new TFGMRemoteDataSourceConfig("data/bus");
    }

    @Override
    protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
        return Collections.singletonList(gtfsSourceConfig);
    }

    @Override
    public boolean getChangeAtInterchangeOnly() {
        return true;
    }

    @Override
    public int getNumberQueries() { return 1; }

    @Override
    public int getQueryInterval() {
        return 15;
    }

    @Override
    public boolean getCreateNeighbours() {
        return false;
    }

    @Override
    public int getMaxWait() {
        return 20;
    }

    @Override
    public int getMaxInitialWait() {
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
    public List<RemoteDataSourceConfig> getRemoteDataSourceConfig() {
        return Arrays.asList(remoteDataSourceConfig, remoteNaptanConfig);
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

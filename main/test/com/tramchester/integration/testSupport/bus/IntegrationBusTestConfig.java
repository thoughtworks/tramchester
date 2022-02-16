package com.tramchester.integration.testSupport.bus;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.GraphDBTestConfig;
import com.tramchester.integration.testSupport.IntegrationTestConfig;
import com.tramchester.integration.testSupport.tfgm.TFGMGTFSSourceTestConfig;
import com.tramchester.integration.testSupport.tfgm.TFGMRemoteDataSourceConfig;
import com.tramchester.testSupport.TestEnv;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class IntegrationBusTestConfig extends IntegrationTestConfig {
    private final GTFSSourceConfig gtfsSourceConfig;
    private final RemoteDataSourceConfig remoteDataSourceConfig;

    public IntegrationBusTestConfig() {
        this("int_test_bus.db");
    }

    public IntegrationBusTestConfig(String dbName) {
        this("integrationBusTest", dbName);
    }

    protected IntegrationBusTestConfig(String folder, String dbName) {
        super(new GraphDBIntegrationBusTestConfig(folder, dbName));

        final Set<TransportMode> modesWithPlatforms = Collections.emptySet();
        final IdSet<Station> additionalInterchanges = IdSet.emptySet();
        final Set<TransportMode> compositeStationModes = Collections.singleton(TransportMode.Bus);

        gtfsSourceConfig = new TFGMGTFSSourceTestConfig("data/bus",
                Collections.singleton(GTFSTransportationType.bus),
                modesWithPlatforms, additionalInterchanges, compositeStationModes, Collections.emptyList());
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

//    @Override
//    public boolean getCreateNeighbours() {
//        return false;
//    }

    @Override
    public boolean hasNeighbourConfig() {
        return false;
    }

    @Override
    public int getMaxWait() {
        return 35;
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
        return Arrays.asList(remoteDataSourceConfig, remoteNaptanXMLConfig, postCodeDatasourceConfig, remoteNPTGconfig);
    }

    @Override
    public Path getCacheFolder() {
        return TestEnv.CACHE_DIR.resolve("busIntegration");
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

package com.tramchester.integration.testSupport;

import com.tramchester.config.RailConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.BoundingBox;
import com.tramchester.integration.testSupport.rail.TestRailConfig;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.testSupport.TestEnv;

import java.util.*;

public class TramAndTrainGreaterManchesterConfig extends IntegrationTramTestConfig {

    private static final String DB_NAME = "int_test_gm_tram_train.db";

    public TramAndTrainGreaterManchesterConfig() {
        super(new TramAndTrainDBTestConfig(), true, Collections.emptyList());
    }

    @Override
    public List<RemoteDataSourceConfig> getRemoteDataSourceConfig() {
        return Arrays.asList(railRemoteDataSource, remoteTFGMConfig, remoteNaptanXMLConfig, remoteNPTGconfig);
    }

    @Override
    public RailConfig getRailConfig() {
        return new TestRailConfig(railRemoteDataSource);
    }

    @Override
    public Set<TransportMode> getTransportModes() {
        return new HashSet<>(Arrays.asList(TransportMode.Tram, TransportMode.Train));
    }

    @Override
    public BoundingBox getBounds() {
        return TestEnv.getGreaterManchester();
    }

    private static class TramAndTrainDBTestConfig extends GraphDBTestConfig {
        public TramAndTrainDBTestConfig() {
            super("integrationTramTrainGMTest", DB_NAME);
        }

        @Override
        public String getNeo4jPagecacheMemory() {
            return "1000m";
        }
    }


}

package com.tramchester.integration.testSupport;

import com.tramchester.config.NeighbourConfig;
import com.tramchester.config.OpenLdbConfig;
import com.tramchester.config.RailConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.BoundingBox;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.integration.testSupport.rail.TestRailConfig;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.testSupport.NeighbourTestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TestOpenLdbConfig;
import com.tramchester.testSupport.reference.TramStations;

import java.nio.file.Path;
import java.util.*;

public class TramAndTrainGreaterManchesterConfig extends IntegrationTramTestConfig {

    private static final String DB_NAME = "int_test_gm_tram_train.db";

    public TramAndTrainGreaterManchesterConfig() {
        super(new TramAndTrainDBTestConfig(), true, Collections.singletonList(IntegrationTestConfig.piccGardensClosed));
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

    @Override
    public int getNumberQueries() {
        return 3;
    }

    @Override
    public int getQueryInterval() {
        return 10;
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

    @Override
    public boolean hasNeighbourConfig() {
        return true;
    }

    @Override
    public NeighbourConfig getNeighbourConfig() {
        NeighbourTestConfig config = new NeighbourTestConfig(0.2D, 3);
        config.addNeighbours(TramStations.EastDidsbury.getId(), RailStationIds.EastDidsbury.getId());
        config.addNeighbours(TramStations.Eccles.getId(), RailStationIds.Eccles.getId());
        config.addNeighbours(TramStations.Ashton.getId(), RailStationIds.Ashton.getId());
        return config;
    }

    @Override
    public Path getCacheFolder() {
        return TestEnv.CACHE_DIR.resolve("tramTrainIntegration");
    }

    @Override
    public OpenLdbConfig getOpenldbwsConfig() {
        return new TestOpenLdbConfig();
    }
}

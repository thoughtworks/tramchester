package com.tramchester.integration.testSupport.rail;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.GraphDBConfig;
import com.tramchester.config.RailConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.geo.BoundingBox;
import com.tramchester.integration.testSupport.GraphDBTestConfig;
import com.tramchester.integration.testSupport.IntegrationTestConfig;
import com.tramchester.testSupport.TestEnv;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class IntegrationRailTestConfig extends IntegrationTestConfig {

    private final GraphDBConfig graphDBConfig;

    public IntegrationRailTestConfig() {
        this("rail_tramchester.db");
    }

    protected IntegrationRailTestConfig(String dbFilename) {
        super(new GraphDBIntegrationRailTestConfig("integrationTrainTest", dbFilename));
        graphDBConfig = new GraphDBIntegrationRailTestConfig("integrationTrainTest", dbFilename);
    }

    private final String CURRENT_PREFIX = "ttisf194";

    @Override
    public RailConfig getRailConfig() {
        return new RailConfig() {
            @Override
            public Path getDataPath() {
                return Path.of("data/rail");
            }

            @Override
            public Path getStations() {
                return Path.of(CURRENT_PREFIX +".msn");
            }

            @Override
            public Path getTimetable() {
                return Path.of(CURRENT_PREFIX+".mca");
            }
        };
    }

    @Override
    public BoundingBox getBounds() {
        return TestEnv.getTrainBounds();
    }

    @Override
    protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
        return Collections.emptyList();
    }

    @Override
    public List<RemoteDataSourceConfig> getRemoteDataSourceConfig() {
        return Collections.emptyList();
    }

    @Override
    public boolean getChangeAtInterchangeOnly() {
        return true;
    }

    @Override
    public GraphDBConfig getGraphDBConfig() {
        return graphDBConfig;
    }

    @Override
    public int getMaxJourneyDuration() {
        return 840;
    }

    @Override
    public int getMaxInitialWait() {
        return 35;
    }

    @Override
    public int getMaxWait() {
        return 25;
    }

    @Override
    public int getNumberQueries() { return 6; }

    @Override
    public int getQueryInterval() { return 10; }

    @Override
    public Path getCacheFolder() {
        return TestEnv.CACHE_DIR.resolve("railIntegration");
    }

    private static class GraphDBIntegrationRailTestConfig extends GraphDBTestConfig {

        public GraphDBIntegrationRailTestConfig(String folder, String dbName) {
            super(folder, dbName);
        }

        @Override
        public String getNeo4jPagecacheMemory() {
            return "1000m";
        }
    }
}
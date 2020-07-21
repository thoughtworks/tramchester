package com.tramchester.integration;

import com.tramchester.config.DataSourceConfig;
import com.tramchester.domain.GTFSTransportationType;
import com.tramchester.geo.BoundingBox;
import com.tramchester.testSupport.TestConfig;
import com.tramchester.testSupport.TestEnv;

import javax.validation.Valid;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class IntegrationTrainTestConfig extends TestConfig {
    private final Path dbPath;
    private final boolean exists;

    public IntegrationTrainTestConfig() {
        this("train_tramchester.db");
    }

    private IntegrationTrainTestConfig(String dbName) {
        this.dbPath = Path.of("databases", "integrationTrainTest", dbName);
        exists = Files.exists(dbPath);
    }

    @Override
    public Set<GTFSTransportationType> getTransportModes() {
        return Collections.singleton(GTFSTransportationType.train);
    }

    @Override
    public @Valid BoundingBox getBounds() {
        return TestEnv.getTrainBounds();
    }

    @Override
    protected DataSourceConfig getTestDataSourceConfig() {
        return new RailTestDataSourceConfig();
    }

    @Override
    public boolean getChangeAtInterchangeOnly() {
        return false;
    }

    @Override
    public boolean getRebuildGraph() {
        return !exists;
    }

    @Override
    public String getGraphName() {
        return dbPath.toAbsolutePath().toString();
    }

    @Override
    public int getNumberQueries() { return 3; }

    @Override
    public int getQueryInterval() {
        return 12;
    }

    @Override
    public double getDistanceToNeighboursKM() {
        return 0.4;
    }

    private static class RailTestDataSourceConfig implements DataSourceConfig {
        public static final String RAIL_LATEST_ZIP = "https://s3.eu-west-2.amazonaws.com/feeds.planar.network/gb-rail-latest.zip";

        // https://planar.network/projects/feeds

        @Override
        public String getTramDataUrl() {
            return RAIL_LATEST_ZIP;
        }

        @Override
        public String getTramDataCheckUrl() {
            return RAIL_LATEST_ZIP;
        }

        @Override
        public Path getDataPath() {
            return Paths.get("data/trains");
        }

        @Override
        public Path getUnzipPath() {
            return Paths.get("./");
        }

        @Override
        public String getZipFilename() {
            return "data.zip";
        }

        @Override
        public String getName() {
            return "rail";
        }

        @Override
        public boolean getHasFeedInfo() {
            return false;
        }

        @Override
        public Set<GTFSTransportationType> getTransportModes() {
            return Collections.singleton(GTFSTransportationType.train);
        }
    }
}

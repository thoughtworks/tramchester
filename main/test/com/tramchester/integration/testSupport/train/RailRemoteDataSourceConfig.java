package com.tramchester.integration.testSupport.train;

import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.domain.DataSourceID;

import java.nio.file.Path;

public class RailRemoteDataSourceConfig implements RemoteDataSourceConfig {
    private static final String RAIL_LATEST_ZIP = "https://s3.eu-west-2.amazonaws.com/feeds.planar.network/gb-rail-latest.zip";

    private final String dataPath;

    public RailRemoteDataSourceConfig(String dataPath) {
        this.dataPath = dataPath;
    }

    @Override
    public Path getDataPath() {
        return Path.of(dataPath);
    }

    @Override
    public String getDataCheckUrl() {
        return RAIL_LATEST_ZIP;
    }

    @Override
    public String getDataUrl() {
        return RAIL_LATEST_ZIP;
    }

    @Override
    public String getDownloadFilename() {
        return "gb-rail-latest.zip";
    }

    @Override
    public String getName() {
        return "gbRailIntegrationTest";
    }

    @Override
    public DataSourceID getDataSourceId() {
        return DataSourceID.gbRail;
    }
}

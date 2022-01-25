package com.tramchester.integration.testSupport.rail;

import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.domain.DataSourceID;

import java.nio.file.Path;

public class RailRemoteDataSourceConfig implements RemoteDataSourceConfig {

    // http://data.atoc.org/how-to

    private static final String CURRENT_PREFIX = "ttis236";

    private static final String RAIL_LATEST_ZIP = String.format("s3://tramchester2dist/railData/%s.zip", CURRENT_PREFIX);

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
        return CURRENT_PREFIX+".zip";
    }

    @Override
    public String getName() {
        return "rail";
    }

    @Override
    public DataSourceID getDataSourceId() {
        return DataSourceID.rail;
    }

    @Override
    public boolean getIsS3() {
        return true;
    }

    public String getFilePrefix() {
        return CURRENT_PREFIX.replace("ttis", "ttisf");
    }
}

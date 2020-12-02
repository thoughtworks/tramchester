package com.tramchester.integration;

import com.tramchester.config.DataSourceConfig;
import com.tramchester.domain.reference.GTFSTransportationType;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;

class RailTestDataSourceConfig implements DataSourceConfig {
    private static final String RAIL_LATEST_ZIP = "https://s3.eu-west-2.amazonaws.com/feeds.planar.network/gb-rail-latest.zip";

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

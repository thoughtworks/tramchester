package com.tramchester.integration;

import com.tramchester.config.DataSourceConfig;

import java.nio.file.Path;
import java.nio.file.Paths;

public class TFGMTestDataSourceConfig implements DataSourceConfig {

    private final String dataFolder;

    public TFGMTestDataSourceConfig(String dataFolder) {
        this.dataFolder = dataFolder;
    }

    @Override
    public String getTramDataUrl() {
        return "http://odata.tfgm.com/opendata/downloads/TfGMgtfs.zip";
    }

    @Override
    public String getTramDataCheckUrl() {
        return null;
    }

    @Override
    public Path getDataPath() {
        return Paths.get(dataFolder);
    }

    @Override
    public Path getUnzipPath() {
        return  Paths.get("gtdf-out");
    }

    @Override
    public String getZipFilename() {
        return "data.zip";
    }

    @Override
    public String getName() {
        return "TFGMTestDataSourceConfig";
    }
}

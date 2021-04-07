package com.tramchester.integration.testSupport;

import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.testSupport.TestEnv;

import java.nio.file.Path;

public class TFGMRemoteDataSourceConfig implements RemoteDataSourceConfig {
    private final String dataPath;

    public TFGMRemoteDataSourceConfig(String dataPath) {
        this.dataPath = dataPath;
    }

    @Override
    public Path getDataPath() {
        return Path.of(dataPath);
    }

    @Override
    public String getDataCheckUrl() {
        return TestEnv.TFGM_TIMETABLE_URL;
    }

    @Override
    public String getDataUrl() {
        return TestEnv.TFGM_TIMETABLE_URL;
    }

    @Override
    public String getDownloadFilename() {
        return "tfgmData.zip";
    }

    @Override
    public String getName() {
        return "intergationTestRemoteSource";
    }
}

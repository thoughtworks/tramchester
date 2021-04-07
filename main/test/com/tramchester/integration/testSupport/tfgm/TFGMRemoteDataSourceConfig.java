package com.tramchester.integration.testSupport.tfgm;

import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.testSupport.TestEnv;

import java.nio.file.Path;

public class TFGMRemoteDataSourceConfig implements RemoteDataSourceConfig {
    private final Path dataPath;

    public TFGMRemoteDataSourceConfig(String dataPath) {
       this(Path.of(dataPath));
    }

    public TFGMRemoteDataSourceConfig(Path dataPath) {
        this.dataPath = dataPath;
    }

    @Override
    public Path getDataPath() {
        return dataPath;
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

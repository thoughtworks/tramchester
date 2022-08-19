package com.tramchester.testSupport.tfgm;

import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.testSupport.TestEnv;

import java.nio.file.Path;

public class TFGMRemoteDataSourceConfig extends RemoteDataSourceConfig {
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
        return "tfgm_data.zip";
    }

    @Override
    public String getName() {
        return "tfgm";
    }

    @Override
    public DataSourceID getDataSourceId() {
        return DataSourceID.tfgm;
    }

    @Override
    public boolean getIsS3() {
        return false;
    }
}

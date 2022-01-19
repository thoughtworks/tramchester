package com.tramchester.integration.testSupport.naptan;

import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.testSupport.TestEnv;

import java.nio.file.Path;

// https://naptan.api.dft.gov.uk/v1/access-nodes?dataFormat=csv

public class NaptanRemoteDataSourceConfig implements RemoteDataSourceConfig {
    private final Path dataPath;

    public NaptanRemoteDataSourceConfig(String dataPath) {
       this(Path.of(dataPath));
    }

    private NaptanRemoteDataSourceConfig(Path dataPath) {
        this.dataPath = dataPath;
    }

    @Override
    public Path getDataPath() {
        return dataPath;
    }

    @Override
    public String getDataCheckUrl() {
        return "";
    }

    @Override
    public String getDataUrl() {
        return TestEnv.NAPTAN_URL;
    }

    @Override
    public String getDownloadFilename() {
        return "Stops.csv";
    }

    @Override
    public String getName() {
        return "naptan";
    }

    @Override
    public boolean getIsS3() {
        return false;
    }
}

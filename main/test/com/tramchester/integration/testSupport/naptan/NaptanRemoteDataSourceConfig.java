package com.tramchester.integration.testSupport.naptan;

import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.testSupport.TestEnv;

import java.nio.file.Path;

// https://naptan.api.dft.gov.uk/v1/access-nodes?dataFormat=csv

public class NaptanRemoteDataSourceConfig implements RemoteDataSourceConfig {
    private final Path dataPath;
    private final String format;

    public NaptanRemoteDataSourceConfig(Path dataPath, boolean xml) {
        this.dataPath = dataPath;
        format = xml ? "xml" : "csv";
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
        // ?dataFormat=csv
        return String.format("%s?dataFormat=%s", TestEnv.NAPTAN_BASE_URL, format);
    }

    @Override
    public String getDownloadFilename() {
        return "Stops." + format;
    }

    @Override
    public String getName() {
        return "naptan"+format;
    }

    @Override
    public boolean getIsS3() {
        return false;
    }
}

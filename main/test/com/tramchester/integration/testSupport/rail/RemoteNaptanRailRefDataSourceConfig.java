package com.tramchester.integration.testSupport.rail;

import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.testSupport.TestEnv;

import java.nio.file.Path;

public class RemoteNaptanRailRefDataSourceConfig implements RemoteDataSourceConfig {

    // naptan api has dropped this file , so need to provide own copy as work around
    // this data is going to go slowly out of date, hopefully naptan will publish again soon

    private static final String RefereneceDataPath = TestEnv.RAIL_REFERENCE_S3_LOCATION;

    private final String dataPath;

    public RemoteNaptanRailRefDataSourceConfig(String dataPath) {
        this.dataPath = dataPath;
    }

    @Override
    public Path getDataPath() {
        return Path.of(dataPath);
    }

    @Override
    public String getDataCheckUrl() {
        return RefereneceDataPath;
    }

    @Override
    public String getDataUrl() {
        return RefereneceDataPath;
    }

    @Override
    public String getDownloadFilename() {
        return "RailReferences.csv";
    }

    @Override
    public String getName() {
        return "railIntegrationTestRailReferences";
    }

    @Override
    public DataSourceID getDataSourceId() {
        return DataSourceID.naptanRailReferenceCSV;
    }

    @Override
    public boolean getIsS3() {
        return true;
    }

}
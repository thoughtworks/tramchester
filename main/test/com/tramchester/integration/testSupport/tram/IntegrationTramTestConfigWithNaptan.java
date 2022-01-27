package com.tramchester.integration.testSupport.tram;

import com.tramchester.config.RemoteDataSourceConfig;

import java.util.Arrays;
import java.util.List;

public class IntegrationTramTestConfigWithNaptan extends IntegrationTramTestConfig {

    public IntegrationTramTestConfigWithNaptan() {
        super();
    }

    public IntegrationTramTestConfigWithNaptan(String dbName) {
        super(dbName);
    }

    @Override
    public List<RemoteDataSourceConfig> getRemoteDataSourceConfig() {
        return Arrays.asList(remoteTFGMConfig, remoteNaptanXMLConfig, remoteNPTGconfig);
    }
}

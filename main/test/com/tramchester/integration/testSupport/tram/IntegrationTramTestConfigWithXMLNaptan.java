package com.tramchester.integration.testSupport.tram;

import com.tramchester.config.RemoteDataSourceConfig;

import java.util.Arrays;
import java.util.List;

public class IntegrationTramTestConfigWithXMLNaptan extends IntegrationTramTestConfig {
    @Override
    public List<RemoteDataSourceConfig> getRemoteDataSourceConfig() {
        return Arrays.asList(remoteTFGMConfig, remoteNaptanXMLConfig);
    }
}

package com.tramchester.integration;

import com.tramchester.config.DataSourceConfig;
import com.tramchester.domain.GTFSTransportationType;
import com.tramchester.testSupport.TestConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class IntegrationTramTestConfig extends IntegrationTestConfig {

    private final DataSourceConfig dataSourceConfig;

    public IntegrationTramTestConfig() {
       this("int_test_tramchester.db");
    }

    public IntegrationTramTestConfig(String dbName) {
        super("integrationTramTest", dbName);
        dataSourceConfig = new TFGMTestDataSourceConfig("data/tram", Collections.singleton(GTFSTransportationType.tram));
    }

    @Override
    protected List<DataSourceConfig> getDataSourceFORTESTING() {
        return Collections.singletonList(dataSourceConfig);
    }

    @Override
    public int getNumberQueries() { return 1; }

    @Override
    public int getQueryInterval() {
        return 6;
    }

}


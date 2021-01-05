package com.tramchester.integration.testSupport;

import com.tramchester.config.DataSourceConfig;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;

import java.util.Collections;
import java.util.List;

public class IntegrationTramTestConfig extends IntegrationTestConfig {

    private final DataSourceConfig dataSourceConfig;

    public IntegrationTramTestConfig() {
       this("int_test_tramchester.db");
    }

    public IntegrationTramTestConfig(String dbName) {
        super("integrationTramTest", dbName);
        dataSourceConfig = new TFGMTestDataSourceConfig("data/tram", Collections.singleton(GTFSTransportationType.tram),
                Collections.singleton(TransportMode.Tram));
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

    @Override
    public String getNeo4jPagecacheMemory() {
        return "100m";
    }

}


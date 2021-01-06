package com.tramchester.integration.testSupport;

import com.tramchester.config.DataSourceConfig;
import com.tramchester.config.LiveDataConfig;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.testSupport.TestLiveDataConfig;

import java.util.Collections;
import java.util.List;

public class IntegrationTramTestConfig extends IntegrationTestConfig {

    private final DataSourceConfig dataSourceConfig;
    private final boolean liveDataEnabled;

    public IntegrationTramTestConfig() {
       this("int_test_tramchester.db", false);
    }

    public IntegrationTramTestConfig(boolean liveDataEnabled) {
        this("int_test_tramchester.db", liveDataEnabled);
    }

    public IntegrationTramTestConfig(String dbName) {
        this(dbName, false);
    }

    private IntegrationTramTestConfig(String dbName, boolean liveDataEnabled) {
        super("integrationTramTest", dbName);
        this.liveDataEnabled = liveDataEnabled;
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

    @Override
    public LiveDataConfig getLiveDataConfig() {
        if (liveDataEnabled) {
            return new TestLiveDataConfig();
        }
        return null;
    }
}


package com.tramchester.unit.graph.calculation;

import com.tramchester.config.DataSourceConfig;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.integration.testSupport.GraphDBTestConfig;
import com.tramchester.integration.testSupport.IntegrationTestConfig;
import com.tramchester.integration.testSupport.TFGMTestDataSourceConfig;

import java.util.Collections;
import java.util.List;

import static com.tramchester.domain.reference.TransportMode.Tram;

class SimpleGraphConfig extends IntegrationTestConfig {

    private static final String TMP_DB = "tmp.db";

    public SimpleGraphConfig() {
        super(new GraphDBTestConfig("unitTest", TMP_DB));
    }

    @Override
    protected List<DataSourceConfig> getDataSourceFORTESTING() {
        TFGMTestDataSourceConfig tfgmTestDataSourceConfig = new TFGMTestDataSourceConfig("data/tram",
                GTFSTransportationType.tram, Tram);
        return Collections.singletonList(tfgmTestDataSourceConfig);
    }

    @Override
    public int getNumberQueries() {
        return 1;
    }

    @Override
    public int getQueryInterval() {
        return 6;
    }
}

package com.tramchester.unit.graph.calculation;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.integration.testSupport.GraphDBTestConfig;
import com.tramchester.integration.testSupport.IntegrationTestConfig;
import com.tramchester.integration.testSupport.tfgm.TFGMGTFSSourceTestConfig;

import java.util.Collections;
import java.util.List;

import static com.tramchester.domain.reference.TransportMode.Tram;

public class SimpleGraphConfig extends IntegrationTestConfig {

    public SimpleGraphConfig(String dbFilename) {
        super(new GraphDBTestConfig("unitTest", dbFilename));
    }

    @Override
    protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
        TFGMGTFSSourceTestConfig tfgmTestDataSourceConfig = new TFGMGTFSSourceTestConfig("data/tram",
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

package com.tramchester.integration.graph.neighbours;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.integration.testSupport.tfgm.TFGMGTFSSourceTestConfig;
import com.tramchester.testSupport.TestEnv;

import java.util.Collections;
import java.util.List;

import static com.tramchester.domain.reference.TransportMode.Tram;

class NeighboursTestConfig extends IntegrationBusTestConfig {

    public NeighboursTestConfig() {
        super("CreateNeighboursTest.db");
    }

    @Override
    protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
        return Collections.singletonList(
                new TFGMGTFSSourceTestConfig("data/bus", TestEnv.tramAndBus,
                        Collections.singleton(Tram)));
    }

    @Override
    public boolean getCreateNeighbours() {
        return true;
    }
}

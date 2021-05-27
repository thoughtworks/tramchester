package com.tramchester.integration.testSupport;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.domain.input.AdditionalTramInterchanges;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.integration.testSupport.tfgm.TFGMGTFSSourceTestConfig;
import com.tramchester.testSupport.TestEnv;

import java.util.Collections;
import java.util.List;

import static com.tramchester.domain.reference.TransportMode.Tram;

public class NeighboursTestConfig extends IntegrationBusTestConfig {

    public NeighboursTestConfig() {
        super("neighboursTest", "createNeighboursTest.db");
    }

    @Override
    protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
        return Collections.singletonList(
                new TFGMGTFSSourceTestConfig("data/bus", TestEnv.tramAndBus,
                        Collections.singleton(Tram), AdditionalTramInterchanges.get()));
    }


    @Override
    public boolean getCreateNeighbours() {
        return true;
    }
}

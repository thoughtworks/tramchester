package com.tramchester.unit.graph.calculation;


import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.tfgm.TFGMGTFSSourceTestConfig;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.tramchester.domain.reference.TransportMode.Tram;

public class SimpleCompositeGraphConfig extends SimpleGraphConfig {
    public SimpleCompositeGraphConfig(String dbFilename) {
        super(dbFilename);
    }

    @Override
    protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
        final Set<TransportMode> compositeStationModes = Collections.singleton(Tram);
        TFGMGTFSSourceTestConfig tfgmTestDataSourceConfig = new TFGMGTFSSourceTestConfig("data/tram",
                GTFSTransportationType.tram, Tram, Collections.emptySet(), compositeStationModes);
        return Collections.singletonList(tfgmTestDataSourceConfig);
    }
}

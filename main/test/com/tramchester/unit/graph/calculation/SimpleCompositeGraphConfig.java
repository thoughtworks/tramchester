package com.tramchester.unit.graph.calculation;


import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.tfgm.TFGMGTFSSourceTestConfig;
import com.tramchester.testSupport.reference.TramStations;

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
        final Set<String> additionalInterchanges = Collections.singleton(TramStations.Cornbrook.forDTO());
        TFGMGTFSSourceTestConfig tfgmTestDataSourceConfig = new TFGMGTFSSourceTestConfig("data/tram",
                GTFSTransportationType.tram, Tram, additionalInterchanges, compositeStationModes, Collections.emptyList());
        return Collections.singletonList(tfgmTestDataSourceConfig);
    }
}

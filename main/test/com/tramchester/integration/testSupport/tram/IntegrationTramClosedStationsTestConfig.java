package com.tramchester.integration.testSupport.tram;

import com.tramchester.domain.StationClosure;

import java.util.List;

public class IntegrationTramClosedStationsTestConfig extends IntegrationTramTestConfig {


    public IntegrationTramClosedStationsTestConfig(String dbName, List<StationClosure> closure) {
        super(dbName,  closure);
    }

    @Override
    public int getMaxNeighbourConnections() {
        return 3;
    }
}

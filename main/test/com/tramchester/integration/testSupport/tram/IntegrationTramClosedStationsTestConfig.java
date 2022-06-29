package com.tramchester.integration.testSupport.tram;

import com.tramchester.domain.StationClosure;

import java.util.List;

public class IntegrationTramClosedStationsTestConfig extends IntegrationTramTestConfig {


    private final boolean planningEnabled;

    public IntegrationTramClosedStationsTestConfig(String dbName, List<StationClosure> closure, boolean planningEnabled) {
        super(dbName,  closure);
        this.planningEnabled = planningEnabled;
    }

    @Override
    public boolean getPlanningEnabled() {
        return planningEnabled;
    }

}

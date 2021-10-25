package com.tramchester.integration.testSupport.tram;


import com.tramchester.resources.APIResource;
import com.tramchester.resources.JourneyPlanningMarker;

public class ResourceTramTestConfig<T extends APIResource>  extends IntegrationTramTestConfig {
    private final boolean planningEnabled;

    public ResourceTramTestConfig(Class<T> resourceClass) {
        planningEnabled =  JourneyPlanningMarker.class.isAssignableFrom(resourceClass);
    }

    public ResourceTramTestConfig(Class<T> resourceClass, boolean liveDataEnabled) {
        super(liveDataEnabled);
        planningEnabled =  JourneyPlanningMarker.class.isAssignableFrom(resourceClass);
    }

    @Override
    public boolean getPlanningEnabled() {
        return planningEnabled;
    }
}

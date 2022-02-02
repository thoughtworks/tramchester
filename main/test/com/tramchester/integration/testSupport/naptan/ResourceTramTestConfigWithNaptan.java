package com.tramchester.integration.testSupport.naptan;


import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfigWithNaptan;
import com.tramchester.resources.APIResource;
import com.tramchester.resources.JourneyPlanningMarker;

public class ResourceTramTestConfigWithNaptan<T extends APIResource>  extends IntegrationTramTestConfigWithNaptan {
    private final boolean planningEnabled;

    public ResourceTramTestConfigWithNaptan(Class<T> resourceClass) {
        planningEnabled =  JourneyPlanningMarker.class.isAssignableFrom(resourceClass);
    }

    @Override
    public boolean getPlanningEnabled() {
        return planningEnabled;
    }
}

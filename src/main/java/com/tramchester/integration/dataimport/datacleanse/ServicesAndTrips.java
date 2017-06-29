package com.tramchester.integration.dataimport.datacleanse;

import java.util.Set;

public class ServicesAndTrips {
    private Set<String> serviceIds;
    private Set<String> tripIds;

    public ServicesAndTrips(Set<String> serviceIds, Set<String> tripIds) {

        this.serviceIds = serviceIds;
        this.tripIds = tripIds;
    }

    public Set<String> getServiceIds() {
        return serviceIds;
    }

    public Set<String> getTripIds() {
        return tripIds;
    }
}

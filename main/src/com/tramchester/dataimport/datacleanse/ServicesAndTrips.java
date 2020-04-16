package com.tramchester.dataimport.datacleanse;

import java.util.Set;

public class ServicesAndTrips {
    private final Set<String> serviceIds;
    private final Set<String> tripIds;

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

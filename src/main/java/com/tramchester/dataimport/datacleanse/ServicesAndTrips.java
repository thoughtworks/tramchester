package com.tramchester.dataimport.datacleanse;

import java.util.List;
import java.util.Set;

public class ServicesAndTrips {
    private Set<String> serviceIds;
    private List<String> tripIds;

    public ServicesAndTrips(Set<String> serviceIds, List<String> tripIds) {

        this.serviceIds = serviceIds;
        this.tripIds = tripIds;
    }

    public Set<String> getServiceIds() {
        return serviceIds;
    }

    public List<String> getTripIds() {
        return tripIds;
    }
}

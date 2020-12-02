package com.tramchester.dataimport.data;

import com.tramchester.domain.IdFor;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.input.Trip;

public class TripData {
    private final IdFor<Route> routeId;
    private final IdFor<Service> serviceId;
    private final IdFor<Trip> tripId;
    private final String tripHeadsign;

    public TripData(String routeId, String serviceId, String tripId, String tripHeadsign) {
        this.routeId = IdFor.createId(removeSpaces(routeId));
        this.serviceId = IdFor.createId(serviceId);
        this.tripId = IdFor.createId(tripId);
        this.tripHeadsign = tripHeadsign.intern();
    }

    private String removeSpaces(String text) {
        return text.replaceAll(" ","");
    }

    public IdFor<Route> getRouteId() {
        return routeId;
    }

    public IdFor<Service> getServiceId() {
        return serviceId;
    }

    public IdFor<Trip> getTripId() {
        return tripId;
    }

    public String getTripHeadsign() {
        return tripHeadsign;
    }

    @Override
    public String toString() {
        return "TripData{" +
                "routeId='" + routeId + '\'' +
                ", serviceId='" + serviceId + '\'' +
                ", tripId='" + tripId + '\'' +
                ", tripHeadsign='" + tripHeadsign + '\'' +
                '}';
    }
}

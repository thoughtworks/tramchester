package com.tramchester.dataimport.data;

public class TripData {
    private final String routeId;
    private final String serviceId;
    private final String tripId;
    private final String tripHeadsign;

    public TripData(String routeId, String serviceId, String tripId, String tripHeadsign) {
        this.routeId = routeId.intern();
        this.serviceId = serviceId.intern();
        this.tripId = tripId.intern();
        this.tripHeadsign = tripHeadsign.intern();
    }

    public String getRouteId() {
        return routeId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getTripId() {
        return tripId;
    }

    public String getTripHeadsign() {
        return tripHeadsign;
    }
}

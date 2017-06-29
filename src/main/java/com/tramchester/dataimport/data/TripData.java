package com.tramchester.dataimport.data;

public class TripData {
    private String routeId;
    private String serviceId;
    private String tripId;
    private String tripHeadsign;

    public TripData(String routeId, String serviceId, String tripId, String tripHeadsign) {
        this.routeId = routeId;
        this.serviceId = serviceId;
        this.tripId = tripId;
        this.tripHeadsign = tripHeadsign;
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

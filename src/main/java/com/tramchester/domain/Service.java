package com.tramchester.domain;


import java.util.ArrayList;
import java.util.List;

public class Service {
    private String serviceId;
    private List<Trip> trips = new ArrayList<>();

    public Service(String serviceId) {
        this.serviceId = serviceId;
    }

    private Service() {
    }

    public String getServiceId() {
        return serviceId;
    }

    public List<Trip> getTrips() {
        return trips;
    }

    public void addTrip(Trip trip) {
        trips.add(trip);
    }
}

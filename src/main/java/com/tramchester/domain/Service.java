package com.tramchester.domain;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Service {
    private String serviceId;
    private List<Trip> trips = new ArrayList<>();
    private HashMap<DaysOfWeek, Boolean> days = new HashMap<>();

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

    public void setDays(boolean monday, boolean tuesday, boolean wednesday, boolean thursday, boolean friday, boolean saturday, boolean sunday) {
        days.put(DaysOfWeek.Monday, monday);
        days.put(DaysOfWeek.Tuesday, tuesday);
        days.put(DaysOfWeek.Wednesday, wednesday);
        days.put(DaysOfWeek.Thursday, thursday);
        days.put(DaysOfWeek.Friday, friday);
        days.put(DaysOfWeek.Saturday, saturday);
        days.put(DaysOfWeek.Sunday, sunday);
    }
}

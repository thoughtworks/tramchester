package com.tramchester.domain;


import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Service {
    private static final Logger logger = LoggerFactory.getLogger(Service.class);
    private final String routeId;

    private String serviceId;
    private List<Trip> trips = new ArrayList<>();
    private HashMap<DaysOfWeek, Boolean> days = new HashMap<>();
    private DateTime startDate;
    private DateTime endDate;

    public Service(String serviceId, String routeId) {
        this.serviceId = serviceId;
        this.routeId = routeId;
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

    public HashMap<DaysOfWeek, Boolean> getDays() {
        return days;
    }

    public List<Trip> getTripsAfter(String firstStationId, String lastStationId, int minutesFromMidnight, int maxNumberOfTrips) {
        logger.info(String.format("Find service %s trips from %s to %s after %s", serviceId, firstStationId, lastStationId, minutesFromMidnight));
        ArrayList<Trip> validTrips = new ArrayList<>();
        StringBuilder tripIds = new StringBuilder();
        for (Trip trip : trips) {
            if (trip.travelsBetween(firstStationId, lastStationId, minutesFromMidnight)) {
                tripIds.append(trip.getTripId() + " ");
                validTrips.add(trip);
            }
        }

        logger.info(String.format("Selected %s of %s trips %s", validTrips.size(), trips.size(), tripIds.toString()));

        int limit = maxNumberOfTrips;
        if (validTrips.size() < maxNumberOfTrips) {
            limit = validTrips.size();
        }
        return validTrips.subList(0, limit);
    }

    @Override
    public String toString() {
        return "Service{" +
                "routeId='" + routeId + '\'' +
                ", serviceId='" + serviceId + '\'' +
                ", trips=" + trips +
                ", days=" + days +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                '}';
    }

    public String getRouteId() {
        return routeId;
    }

    public void setServiceDateRange(DateTime startDate, DateTime endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public DateTime getStartDate() {
        return startDate;
    }

    public DateTime getEndDate() {
        return endDate;
    }
}

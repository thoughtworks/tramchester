package com.tramchester.domain;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.function.ToIntFunction;

public class Service {
    private static final Logger logger = LoggerFactory.getLogger(Service.class);

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

    public HashMap<DaysOfWeek, Boolean> getDays() {
        return days;
    }

    public List<Trip> getTripsAfter(String firstStationId, String lastStationId, int minutesFromMidnight) {
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

//        validTrips.sort(new Comparator<Trip>() {
//            @Override
//            public int compare(Trip o1, Trip o2) {
//                return o1.getStop(firstStationId).getMinutesFromMidnight() - o2.getStop(firstStationId).getMinutesFromMidnight();
//            }
//        });
        int limit = 5;
        if (validTrips.size()<5) {
            limit = validTrips.size();
        }
        return validTrips.subList(0,limit);
    }
}

package com.tramchester.domain;


import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import static com.tramchester.domain.DaysOfWeek.*;

public class Service {

    private final String routeId;
    private String serviceId;
    private Set<Trip> trips = new LinkedHashSet<>();

    private HashMap<DaysOfWeek, Boolean> days = new HashMap<>();
    private TramServiceDate startDate;
    private TramServiceDate endDate;

    public Service(String serviceId, String routeId) {
        this.serviceId = serviceId;
        this.routeId = routeId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public Set<Trip> getTrips() {
        return trips;
    }

    public void addTrip(Trip trip) {
        trips.add(trip);
    }

    public void setDays(boolean monday, boolean tuesday, boolean wednesday, boolean thursday, boolean friday, boolean saturday, boolean sunday) {
        days.put(Monday, monday);
        days.put(Tuesday, tuesday);
        days.put(Wednesday, wednesday);
        days.put(Thursday, thursday);
        days.put(Friday, friday);
        days.put(Saturday, saturday);
        days.put(Sunday, sunday);
    }

    public HashMap<DaysOfWeek, Boolean> getDays() {
        return days;
    }

    public Optional<Trip> getFirstTripAfter(String firstStationId, String lastStationId, TimeWindow timeWindow) {
        return trips.stream().filter(trip -> trip.travelsBetween(firstStationId, lastStationId, timeWindow)).findFirst();
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

    public void setServiceDateRange(LocalDate startDate, LocalDate endDate) {
        this.startDate = new TramServiceDate(startDate);
        this.endDate = new TramServiceDate(endDate);
    }

    public TramServiceDate getStartDate() {
        return startDate;
    }

    public TramServiceDate getEndDate() {
        return endDate;
    }

    public boolean isRunning() {
        return days.get(Monday) ||
                days.get(Tuesday) ||
                days.get(Wednesday) ||
                days.get(Thursday) ||
                days.get(Friday) ||
                days.get(Saturday) ||
                days.get(Sunday);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Service service = (Service) o;

        return !(serviceId != null ? !serviceId.equals(service.serviceId) : service.serviceId != null);
    }

    @Override
    public int hashCode() {
        return serviceId != null ? serviceId.hashCode() : 0;
    }

    public boolean operatesOn(LocalDate date) {
        return operatesOn(startDate, endDate, date);
    }

    public static boolean operatesOn(TramServiceDate startDate, TramServiceDate endDate, LocalDate date) {
        LocalDate begin = startDate.getDate();
        LocalDate end = endDate.getDate();
        if  (date.isAfter(begin) && date.isBefore(end)) {
            return true;
        }
        if (date.equals(begin) || date.equals(end)) {
            return true;
        }
        return false;
    }

}

package com.tramchester.domain;

import com.tramchester.repository.TransportData;
import org.joda.time.LocalDate;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public class StationsOpenToday {
    private TransportData transportData;

    public StationsOpenToday(TransportData transportData) {
        this.transportData = transportData;
    }

    public Set<Location> getStations(LocalDate localDate) {
        return createStationList(localDate);
    }

    private Set<Location> createStationList(LocalDate date) {
        Set<Location> result = new HashSet<>();
        transportData.getRoutes().stream().forEach(route ->
                route.getServices().stream().
                        filter(service -> service.operatesOn(date)).
                        forEach(service -> result.addAll(getStationsFor(service))));
        return result;
    }

    private Set<Location> getStationsFor(Service service) {
        Set<Location> result = new HashSet<>();
        service.getTrips().stream().forEach(trip ->
                trip.getStops().stream()
                    .forEach(stop -> result.add(stop.getStation())));
        return result;
    }

}

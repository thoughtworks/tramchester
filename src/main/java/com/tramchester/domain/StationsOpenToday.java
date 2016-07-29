package com.tramchester.domain;

import com.tramchester.repository.TransportData;
import org.joda.time.LocalDate;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class StationsOpenToday {
    private TransportData transportData;
    private Optional<LocalDate> currentDate;
    private HashSet<Location> openToday;

    public StationsOpenToday(TransportData transportData) {
        this.transportData = transportData;
        currentDate = Optional.empty();
        openToday = new HashSet<>();
    }

    public Set<Location> getStations(LocalDate localDate) {
        return getStationList(localDate);
    }

    private Set<Location> getStationList(LocalDate date) {
        if (currentDate.isPresent()) {
            if (currentDate.get().equals(date)) {
                return openToday;
            }

        }
        openToday.clear();
        transportData.getRoutes().stream().forEach(route ->
                route.getServices().stream().
                        filter(service -> service.operatesOn(date)).
                        forEach(service -> openToday.addAll(getStationsFor(service))));

        return openToday;
    }

    private Set<Location> getStationsFor(Service service) {
        Set<Location> result = new HashSet<>();
        service.getTrips().stream().forEach(trip ->
                trip.getStops().stream()
                    .forEach(stop -> result.add(stop.getStation())));
        return result;
    }

}

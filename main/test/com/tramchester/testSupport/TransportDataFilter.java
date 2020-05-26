package com.tramchester.testSupport;

import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Location;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class TransportDataFilter {

    public static Set<Trip> getTripsFor(Collection<Trip> trips, Location station) {
        Set<Trip> callingTrips = new HashSet<>();
        trips.forEach(trip -> {
            if (trip.getStops().callsAt(station)) {
                callingTrips.add(trip);
            }
        });
        return callingTrips;
    }
}

package com.tramchester.testSupport;

import com.tramchester.domain.input.Trip;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class TransportDataFilter {

    public static Set<Trip> getTripsFor(Collection<Trip> trips, String stationId) {
        Set<Trip> callingTrips = new HashSet<>();
        trips.forEach(trip -> {
            if (trip.callsAt(stationId)) {
                callingTrips.add(trip);
            }
        });
        return callingTrips;
    }
}

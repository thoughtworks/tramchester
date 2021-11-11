package com.tramchester.testSupport;

import com.tramchester.domain.id.HasId;
import com.tramchester.domain.input.MutableTrip;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class TransportDataFilter {

    public static Set<Trip> getTripsFor(Collection<Trip> trips, HasId<Station> station) {
        Set<Trip> callingTrips = new HashSet<>();
        trips.forEach(trip -> {
            if (trip.getStopCalls().callsAt(station)) {
                callingTrips.add(trip);
            }
        });
        return callingTrips;
    }
}

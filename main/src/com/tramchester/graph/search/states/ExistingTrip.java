package com.tramchester.graph.search.states;

import com.tramchester.domain.IdFor;
import com.tramchester.domain.input.Trip;

public class ExistingTrip {
    private final IdFor<Trip> tripId;

    private ExistingTrip() {
        this.tripId = IdFor.invalid();
    }

    private ExistingTrip(IdFor<Trip> tripId) {
        this.tripId = tripId;
    }

    public static ExistingTrip none() {
        return new ExistingTrip();
    }

    public static ExistingTrip onTrip(IdFor<Trip> tripId) {
        return new ExistingTrip(tripId);
    }

    public boolean isOnTrip() {
        return tripId.isValid();
    }

    public IdFor<Trip> getTripId() {
        return tripId;
    }

    @Override
    public String toString() {
        return "ExistingTrip{" +
                "tripId='" + tripId + '\'' +
                '}';
    }
}

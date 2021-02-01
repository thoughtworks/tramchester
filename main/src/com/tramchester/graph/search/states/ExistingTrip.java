package com.tramchester.graph.search.states;

import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.Trip;

public class ExistingTrip {
    private final StringIdFor<Trip> tripId;

    private ExistingTrip() {
        this.tripId = StringIdFor.invalid();
    }

    private ExistingTrip(StringIdFor<Trip> tripId) {
        this.tripId = tripId;
    }

    public static ExistingTrip none() {
        return new ExistingTrip();
    }

    public static ExistingTrip onTrip(StringIdFor<Trip> tripId) {
        return new ExistingTrip(tripId);
    }

    public boolean isOnTrip() {
        return tripId.isValid();
    }

    public StringIdFor<Trip> getTripId() {
        return tripId;
    }

    @Override
    public String toString() {
        return "ExistingTrip{" +
                "tripId='" + tripId + '\'' +
                '}';
    }
}

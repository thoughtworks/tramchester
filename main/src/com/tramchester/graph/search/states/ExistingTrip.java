package com.tramchester.graph.search.states;

public class ExistingTrip {
    private final String tripId;

    private ExistingTrip() {
        this.tripId = "";
    }

    private ExistingTrip(String tripId) {
        this.tripId = tripId;
    }

    public static ExistingTrip none() {
        return new ExistingTrip();
    }

    public static ExistingTrip onTrip(String tripId) {
        return new ExistingTrip(tripId);
    }

    public boolean isOnTrip() {
        return !tripId.isEmpty();
    }

    public String getTripId() {
        return tripId;
    }
}

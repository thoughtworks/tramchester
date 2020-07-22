package com.tramchester.repository;

import com.tramchester.domain.input.Trip;

import java.util.Set;

public interface TripRepository {
    Set<Trip> getTrips();
    Trip getTripById(String tripId);
    boolean hasTripId(String tripId);
}

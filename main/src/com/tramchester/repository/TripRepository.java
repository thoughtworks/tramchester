package com.tramchester.repository;

import com.tramchester.domain.IdFor;
import com.tramchester.domain.input.Trip;

import java.util.Set;

public interface TripRepository {
    Set<Trip> getTrips();
    Trip getTripById(IdFor<Trip> tripId);
    boolean hasTripId(IdFor<Trip> tripId);
}

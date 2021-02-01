package com.tramchester.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;

import java.util.Set;

@ImplementedBy(TransportData.class)
public interface TripRepository {
    Set<Trip> getTrips();
    Trip getTripById(IdFor<Trip> tripId);
    boolean hasTripId(IdFor<Trip> tripId);
}

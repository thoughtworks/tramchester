package com.tramchester.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.Trip;

import java.util.Set;

@ImplementedBy(TransportData.class)
public interface TripRepository {
    Set<Trip> getTrips();
    Trip getTripById(StringIdFor<Trip> tripId);
    boolean hasTripId(StringIdFor<Trip> tripId);
}

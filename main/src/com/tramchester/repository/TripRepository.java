package com.tramchester.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;

import java.util.Set;

@ImplementedBy(TransportData.class)
public interface TripRepository {
    Set<Trip> getTrips();
    Trip getTripById(IdFor<Trip> tripId);
    Set<Trip> getTripsFor(Station station, TramDate date);
}

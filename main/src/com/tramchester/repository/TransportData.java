package com.tramchester.repository;

import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.Station;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.input.Trip;

import java.util.Collection;
import java.util.Set;

public interface TransportData extends ProvidesFeedInfo, StationRepository {
    Collection<Service> getServices();
    Collection<Trip> getTrips();
    Trip getTrip(String tripId);
    Set<Service> getServicesOnDate(TramServiceDate date);
    Collection<Route> getRoutes();
    Route getRoute(String routeId);
    Set<Station> getStations();
}

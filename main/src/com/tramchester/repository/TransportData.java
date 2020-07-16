package com.tramchester.repository;

import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.time.TramServiceDate;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;

public interface TransportData extends ProvidesFeedInfo, StationRepository {
    Collection<Service> getServices();
    Set<Service> getServicesOnDate(TramServiceDate date);

    Collection<Trip> getTrips();
    Trip getTrip(String tripId);

    Collection<Route> getRoutes();
    Route getRoute(String routeId);

    Collection<Agency> getAgencies();

    String getVersion();
}

package com.tramchester.repository;

import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.Station;
import com.tramchester.domain.TramServiceDate;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface TransportData extends ProvidesFeedInfo {
    Collection<Service> getServices();
    Set<Service> getServicesOnDate(TramServiceDate date);
    Collection<Route> getRoutes();
    Route getRoute(String routeId);
    Set<Station> getStations();
}

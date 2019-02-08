package com.tramchester.repository;

import com.tramchester.domain.Route;
import com.tramchester.domain.Station;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface TransportData extends ProvidesFeedInfo {
    Collection<Route> getRoutes();
    Route getRoute(String routeId);
    Set<Station> getStations();
}

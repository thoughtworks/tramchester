package com.tramchester.integration.repository;

import com.tramchester.domain.Route;
import com.tramchester.domain.Station;

import java.util.Collection;
import java.util.List;

public interface TransportData extends ProvidesFeedInfo {
    Collection<Route> getRoutes();
    Route getRoute(String routeId);
    List<Station> getStations();
}

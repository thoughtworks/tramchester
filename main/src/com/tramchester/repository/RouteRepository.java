package com.tramchester.repository;

import com.tramchester.domain.Route;

import java.util.Set;

public interface RouteRepository {
    Set<Route> getRoutes();
    Route getRouteById(String routeId);
    boolean hasRouteId(String routeId);
}

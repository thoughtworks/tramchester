package com.tramchester.repository;

import com.tramchester.domain.IdFor;
import com.tramchester.domain.Route;

import java.util.Set;

public interface RouteRepository {
    Set<Route> getRoutes();
    Route getRouteById(IdFor<Route> routeId);
    boolean hasRouteId(IdFor<Route> routeId);
}

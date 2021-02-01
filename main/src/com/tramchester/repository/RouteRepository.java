package com.tramchester.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.Route;

import java.util.Set;

@ImplementedBy(TransportData.class)
public interface RouteRepository {
    Set<Route> getRoutes();
    Route getRouteById(StringIdFor<Route> routeId);
    boolean hasRouteId(StringIdFor<Route> routeId);
}

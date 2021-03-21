package com.tramchester.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.Agency;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.Route;

import java.util.Set;

@ImplementedBy(TransportData.class)
public interface RouteRepository {
    Set<Route> getRoutes();
    Route getRouteById(IdFor<Route> routeId);
    boolean hasRouteId(IdFor<Route> routeId);

    Route findRouteByShortName(IdFor<Agency> agencyId, String name);
}

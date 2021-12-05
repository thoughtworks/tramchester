package com.tramchester.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;

import java.time.LocalDate;
import java.util.Set;

@ImplementedBy(TransportData.class)
public interface RouteRepository {
    Set<Route> getRoutes();
    Route getRouteById(IdFor<Route> routeId);
    int numberOfRoutes();

    Set<Route> findRoutesByShortName(IdFor<Agency> agencyId, String shortName);
    Set<Route> findRoutesByName(IdFor<Agency> agencyId, String longName);

    IdSet<Route> getRoutesRunningOn(LocalDate date);
}

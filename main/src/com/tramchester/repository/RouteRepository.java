package com.tramchester.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.ReadonlyAgency;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;

import java.util.Set;

@ImplementedBy(TransportData.class)
public interface RouteRepository {
    Set<Route> getRoutes();
    Route getRouteById(IdFor<Route> routeId);
    int numberOfRoutes();

    Set<Route> findRoutesByShortName(IdFor<ReadonlyAgency> agencyId, String shortName);
    Set<Route> findRoutesByName(IdFor<ReadonlyAgency> agencyId, String longName);

}

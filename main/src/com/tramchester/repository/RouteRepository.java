package com.tramchester.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.Agency;
import com.tramchester.domain.RouteReadOnly;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.Route;

import java.util.Set;

@ImplementedBy(TransportData.class)
public interface RouteRepository {
    Set<RouteReadOnly> getRoutes();
    RouteReadOnly getRouteById(IdFor<RouteReadOnly> routeId);
    boolean hasRouteId(IdFor<RouteReadOnly> routeId);
    int numberOfRoutes();

    Set<RouteReadOnly> findRoutesByShortName(IdFor<Agency> agencyId, String shortName);
    Set<RouteReadOnly> findRoutesByName(IdFor<Agency> agencyId, String longName);

}

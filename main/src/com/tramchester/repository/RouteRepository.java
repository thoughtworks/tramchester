package com.tramchester.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.reference.TransportMode;

import java.time.LocalDate;
import java.util.Set;

@ImplementedBy(TransportData.class)
public interface RouteRepository extends NumberOfRoutes {
    Set<Route> getRoutes();
    Set<Route> getRoutes(Set<TransportMode> mode);

    Route getRouteById(IdFor<Route> routeId);

    Set<Route> findRoutesByShortName(IdFor<Agency> agencyId, String shortName);
    Set<Route> findRoutesByName(IdFor<Agency> agencyId, String longName);

    Set<Route> getRoutesRunningOn(TramDate date);
}

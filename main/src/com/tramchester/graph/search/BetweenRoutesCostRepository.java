package com.tramchester.graph.search;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.NumberOfChanges;
import com.tramchester.domain.RouteReadOnly;
import com.tramchester.domain.places.Station;

import java.util.Set;

@ImplementedBy(RouteToRouteCosts.class)
public interface BetweenRoutesCostRepository {
    int getFor(RouteReadOnly routeA, RouteReadOnly routeB);
    int size();

    NumberOfChanges getNumberOfChanges(Set<Station> starts, Set<Station> destinations);
    NumberOfChanges getNumberOfChanges(Station startStation, Station destination);

    LowestCostsForRoutes getLowestCostCalcutatorFor(Set<Station> desintationRoutes);
}

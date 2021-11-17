package com.tramchester.graph.search;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.NumberOfChanges;
import com.tramchester.domain.Route;
import com.tramchester.domain.places.Station;

import java.util.Set;

@ImplementedBy(RouteToRouteCosts.class)
public interface BetweenRoutesCostRepository {
    int getFor(Route routeA, Route routeB);
    //int size();

    NumberOfChanges getNumberOfChanges(Set<Station> starts, Set<Station> destinations);
    NumberOfChanges getNumberOfChanges(Station startStation, Station destination);

    LowestCostsForRoutes getLowestCostCalcutatorFor(Set<Station> desintationRoutes);
}

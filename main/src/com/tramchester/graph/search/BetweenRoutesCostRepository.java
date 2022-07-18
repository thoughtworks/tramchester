package com.tramchester.graph.search;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.NumberOfChanges;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;

import java.time.LocalDate;
import java.util.Set;

@ImplementedBy(RouteToRouteCosts.class)
public interface BetweenRoutesCostRepository {
    //int getFor(Route routeA, Route routeB);

    NumberOfChanges getNumberOfChanges(LocationSet starts, LocationSet destinations, LocalDate date, TimeRange time);
    NumberOfChanges getNumberOfChanges(Location<?> start, Location<?> destination, Set<TransportMode> modes, LocalDate date, TimeRange time);
    NumberOfChanges getNumberOfChanges(StationGroup start, StationGroup end, LocalDate date, TimeRange time);

    LowestCostsForDestRoutes getLowestCostCalcutatorFor(LocationSet desintationRoutes, LocalDate date, TimeRange time);

}

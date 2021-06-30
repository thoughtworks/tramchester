package com.tramchester.graph.search;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;

import java.util.stream.Stream;

@ImplementedBy(RouteToRouteCosts.class)
public interface BetweenRoutesCostRepository {
    int getFor(Route routeA, Route routeB);
    <T extends HasId<Route>> Stream<T> sortByDestinations(Stream<T> startingRoutes, IdSet<Route> destinationRouteIds);
    int size();

    int minRouteHops(Station start, Station end);
    int maxRouteHops(Station start, Station end);
}

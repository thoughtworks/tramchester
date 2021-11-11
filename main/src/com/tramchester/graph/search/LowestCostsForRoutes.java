package com.tramchester.graph.search;

import com.tramchester.domain.RouteReadOnly;
import com.tramchester.domain.id.HasId;

import java.util.stream.Stream;

public interface LowestCostsForRoutes {
    int getFewestChanges(RouteReadOnly currentRoute);
    <T extends HasId<RouteReadOnly>> Stream<T> sortByDestinations(Stream<T> startingRoutes);
}

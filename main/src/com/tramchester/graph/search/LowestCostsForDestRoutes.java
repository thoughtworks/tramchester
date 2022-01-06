package com.tramchester.graph.search;

import com.tramchester.domain.Route;
import com.tramchester.domain.id.HasId;

import java.util.stream.Stream;

public interface LowestCostsForDestRoutes {
    int getFewestChanges(Route currentRoute);
    <T extends HasId<Route>> Stream<T> sortByDestinations(Stream<T> startingRoutes);
}

package com.tramchester.graph.search.routes;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.collections.IndexedBitSet;
import com.tramchester.domain.collections.RouteIndexPair;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.reference.TransportMode;

import java.util.Set;

@ImplementedBy(RouteCostMatrix.class)
public interface RouteCostCombinations {

    // create a bitmask for route->route changes that are possible on a given date and transport mode
    IndexedBitSet createOverlapMatrixFor(TramDate date, Set<TransportMode> requestedModes);

    long size();

    RouteCostMatrix.AnyOfPaths getInterchangesFor(RouteIndexPair indexPair, IndexedBitSet dateOverlaps);

    int getMaxDepth();

    int getDepth(RouteIndexPair routePair);

}

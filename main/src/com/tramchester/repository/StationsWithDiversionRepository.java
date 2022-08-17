package com.tramchester.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.graph.AddWalksForClosedGraphBuilder;

import java.util.Set;

@ImplementedBy(AddWalksForClosedGraphBuilder.class)
public interface StationsWithDiversionRepository {
    boolean hasDiversions(Station station);
    Set<DateRange> getDateRangesFor(Station station);
}

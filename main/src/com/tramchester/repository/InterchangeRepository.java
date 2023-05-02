package com.tramchester.repository;

import com.google.inject.ImplementedBy;
import com.netflix.governator.InjectorBuilder;
import com.tramchester.domain.collections.RouteIndexPair;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;

import java.util.Set;
import java.util.stream.Stream;

@ImplementedBy(Interchanges.class)
public interface InterchangeRepository {
    boolean isInterchange(Location<?> location);
    Set<InterchangeStation> getAllInterchanges();
    int size();

    InterchangeStation getInterchange(Location<?> location);

    boolean isInterchange(IdFor<Station> stationId);

    boolean hasInterchangeFor(RouteIndexPair indexPair);

    Stream<InterchangeStation> getInterchangesFor(RouteIndexPair indexPair);
}

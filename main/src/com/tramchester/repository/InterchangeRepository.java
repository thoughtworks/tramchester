package com.tramchester.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;

import java.util.Set;

@ImplementedBy(Interchanges.class)
public interface InterchangeRepository {
    boolean isInterchange(Station station);

    IdSet<Station> getInterchangesFor(TransportMode mode);

    Set<Station> getAllInterchanges();

    Set<Station> getInterchangesOn(Route route);

    int size();
}

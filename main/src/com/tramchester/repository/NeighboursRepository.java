package com.tramchester.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.StationLink;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;

import java.util.Set;

@ImplementedBy(Neighbours.class)
public interface NeighboursRepository {
    boolean differentModesOnly();

    Set<StationLink> getAll();
    Set<Station> getNeighboursFor(IdFor<Station> id);
    boolean hasNeighbours(IdFor<Station> id);
}

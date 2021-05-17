package com.tramchester.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.StationLink;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.CreateNeighbours;

import java.util.List;

@ImplementedBy(CreateNeighbours.class)
public interface NeighboursRepository {
    IdSet<Station> getStationsWithNeighbours(TransportMode mode);
    List<StationLink> getAll();
    IdSet<Station> getNeighboursFor(IdFor<Station> id);
}

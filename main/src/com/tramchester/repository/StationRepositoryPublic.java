package com.tramchester.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;

import java.util.Set;
import java.util.stream.Stream;

@ImplementedBy(TransportData.class)
public interface StationRepositoryPublic {
    Set<Station> getStationsServing(TransportMode mode);
    Station getStationById(IdFor<Station> stationId);
    boolean hasStationId(IdFor<Station> stationId);
    Stream<Station> getActiveStationStream();
}

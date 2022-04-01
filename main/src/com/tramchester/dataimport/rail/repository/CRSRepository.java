package com.tramchester.dataimport.rail.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.places.Station;

@ImplementedBy(RailStationCRSRepository.class)
public interface CRSRepository {
    String getCRSFor(Station station);
    boolean hasStation(Station station);
}

package com.tramchester.dataimport.rail.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.places.Station;

/***
 * Supports live rail data which uses the CRS id, not tiploc
 */
@ImplementedBy(RailStationCRSRepository.class)
public interface CRSRepository {
    String getCRSFor(Station station);
    boolean hasStation(Station station);

    Station getFor(String crs);
    boolean hasCrs(String crs);
}

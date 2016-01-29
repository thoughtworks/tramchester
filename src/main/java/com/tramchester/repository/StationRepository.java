package com.tramchester.repository;

import com.tramchester.domain.Station;

public interface StationRepository {
    Station getStation(String stationId);
}

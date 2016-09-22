package com.tramchester.repository;

import com.tramchester.domain.Station;

import java.util.List;
import java.util.Optional;

public interface StationRepository {
    Optional<Station> getStation(String stationId);
    List<Station> getStations();
}

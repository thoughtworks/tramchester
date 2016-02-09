package com.tramchester.repository;

import com.tramchester.domain.Station;

import java.util.List;

public interface StationRepository {
    Station getStation(String stationId);
    List<Station> getStations();
}

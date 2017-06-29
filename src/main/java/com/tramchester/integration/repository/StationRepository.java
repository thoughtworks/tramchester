package com.tramchester.integration.repository;

import com.tramchester.domain.Route;
import com.tramchester.domain.Station;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StationRepository {
    Optional<Station> getStation(String stationId);
    List<Station> getStations();
    Collection<Route> getRoutes();
}

package com.tramchester.repository;

import com.tramchester.domain.RouteStation;
import com.tramchester.domain.Station;

import java.util.Optional;
import java.util.Set;

public interface StationRepository {
    boolean hasStationId(String stationId);

    Station getStation(String stationId);
    Optional<Station> getStationByName(String name);
    Set<Station> getStations();

    Set<RouteStation> getRouteStations();
    RouteStation getRouteStation(String routeStationId);

}

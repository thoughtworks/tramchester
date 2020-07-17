package com.tramchester.repository;

import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;

import java.util.Optional;
import java.util.Set;

public interface StationRepository {
    boolean hasStationId(String stationId);

    Station getStation(String stationId);
    Set<Station> getStations();

    // live data association
    Optional<Station> getTramStationByName(String name);

    Set<RouteStation> getRouteStations();
    RouteStation getRouteStation(String routeStationId);

}

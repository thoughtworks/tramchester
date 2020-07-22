package com.tramchester.repository;

import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;

import java.util.Optional;
import java.util.Set;

public interface StationRepository {
    Set<Station> getStations();
    Station getStationById(String stationId);
    boolean hasStationId(String stationId);

    // live data association
    Optional<Station> getTramStationByName(String name);

    Set<RouteStation> getRouteStations();
    RouteStation getRouteStationById(String routeStationId);

}

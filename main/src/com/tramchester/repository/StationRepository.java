package com.tramchester.repository;

import com.tramchester.domain.IdFor;
import com.tramchester.domain.Route;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;

import java.util.Optional;
import java.util.Set;

public interface StationRepository {
    Set<Station> getStations();
    Set<Station> getStationsForMode(TransportMode mode);

    Station getStationById(IdFor<Station> stationId);
    boolean hasStationId(IdFor<Station> stationId);

    // live data association
    Optional<Station> getTramStationByName(String name);

    Set<RouteStation> getRouteStations();

    RouteStation getRouteStationById(IdFor<RouteStation> routeStationId);
    RouteStation getRouteStation(Station startStation, Route route);

}

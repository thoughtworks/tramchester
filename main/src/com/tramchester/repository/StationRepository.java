package com.tramchester.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.Route;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@ImplementedBy(TransportData.class)
public interface StationRepository {
    Set<Station> getStations();
    Set<Station> getStationsForMode(TransportMode mode);
    int getNumberOfStations();

    Station getStationById(IdFor<Station> stationId);
    boolean hasStationId(IdFor<Station> stationId);

    // live data association
    Optional<Station> getTramStationByName(String name);

    /***
     * Use with care, return includes stations due to rare circumstances, such as return to deport
     * Nomrally use RouteCallingStations instead if you want stations for a 'normally' defined route
     * @return all route stations
     */
    Set<RouteStation> getRouteStations();

    RouteStation getRouteStationById(IdFor<RouteStation> routeStationId);
    RouteStation getRouteStation(Station startStation, Route route);

}

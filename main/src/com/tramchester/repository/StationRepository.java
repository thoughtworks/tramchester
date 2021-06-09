package com.tramchester.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.Route;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@ImplementedBy(TransportData.class)
public interface StationRepository extends StationRepositoryPublic {

    int getNumberOfStations();
    Set<Station> getStations();

    // live data association
    Optional<Station> getTramStationByName(String name);

    /***
     * Use with care, return includes route stations present due to rare circumstances, such as return to depot
     * Normally use class RouteCallingStations instead if you want stations for a 'normally' defined route
     * @return all route stations
     */
    Set<RouteStation> getRouteStations();

    RouteStation getRouteStationById(IdFor<RouteStation> routeStationId);
    RouteStation getRouteStation(Station station, Route route);

    Stream<Station> getStationsForModeStream(TransportMode mode);

    Set<RouteStation> getRouteStationsFor(IdFor<Station> stationId);
}

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
public interface StationRepository extends StationRepositoryPublic {

    int getNumberOfStations();
    Set<Station> getStations();

    // live data association
    Optional<Station> getTramStationByName(String name);

    /***
     * Use with care, return includes route stations present due to rare circumstances, such as return to depot
     * Nomrally use RouteCallingStations instead if you want stations for a 'normally' defined route
     * @return all route stations
     */
    Set<RouteStation> getRouteStations();

    RouteStation getRouteStationById(IdFor<RouteStation> routeStationId);
    RouteStation getRouteStation(Station startStation, Route route);

}

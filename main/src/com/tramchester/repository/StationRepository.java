package com.tramchester.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Stream;

@ImplementedBy(TransportData.class)
public interface StationRepository extends StationRepositoryPublic {

    long getNumberOfStations(DataSourceID dataSourceID, TransportMode mode);

    Set<Station> getStations();

    Set<Station> getStations(EnumSet<TransportMode> modes);

    /***
     * Use with care, return includes route stations present due to rare circumstances, such as return to depot
     * Normally use class RouteCallingStations instead if you want stations for a 'normally' defined route.
     * @return all route stations
     */
    Set<RouteStation> getRouteStations();

    RouteStation getRouteStationById(IdFor<RouteStation> routeStationId);
    RouteStation getRouteStation(Station station, Route route);

    Set<RouteStation> getRouteStationsFor(IdFor<Station> stationId);

    Stream<Station> getStationsFromSource(DataSourceID dataSourceID);

    /***
     * Includes 'inactive' stations, those with no pickup or dropoff's present
     * @return stream of stations
     */
    Stream<Station> getAllStationStream();
}

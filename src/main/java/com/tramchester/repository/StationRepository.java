package com.tramchester.repository;

import com.tramchester.domain.Route;
import com.tramchester.domain.Station;
import com.tramchester.domain.input.Trip;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface StationRepository {
    Optional<Station> getStation(String stationId);
    List<Station> getStations();
    Collection<Route> getRoutes();
    Stream<Trip> getTripsByRouteId(String routeId);
}

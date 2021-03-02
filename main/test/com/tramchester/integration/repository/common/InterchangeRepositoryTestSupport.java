package com.tramchester.integration.repository.common;

import com.tramchester.domain.Route;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.StationRepository;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class InterchangeRepositoryTestSupport {


    public static Set<Route> RoutesWithInterchanges(InterchangeRepository interchangeRepository, StationRepository stationRepository,
                                                    TransportMode mode) {
        return interchangeRepository.getInterchangesFor(mode).stream().
                map(stationRepository::getStationById).
                map(Station::getRoutes).flatMap(Collection::stream).collect(Collectors.toSet());
    }
}

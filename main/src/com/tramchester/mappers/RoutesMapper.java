package com.tramchester.mappers;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.RouteDTO;
import com.tramchester.domain.presentation.DTO.LocationRefWithPosition;
import com.tramchester.domain.presentation.DTO.RouteRefDTO;
import com.tramchester.domain.presentation.DTO.factory.DTOFactory;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.TransportData;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

@LazySingleton
public class RoutesMapper {
    private static final Logger logger = LoggerFactory.getLogger(RoutesMapper.class);

    private final RouteRepository routeRepository;
    private final DTOFactory DTOFactory;

    @Inject
    public RoutesMapper(RouteRepository routeRepository, DTOFactory DTOFactory) {
        this.DTOFactory = DTOFactory;
        this.routeRepository = routeRepository;
    }

    @PostConstruct
    private void start() {
        logger.info("Starting");

        logger.info("started");
    }


    @PreDestroy
    private void stop() {

    }

    public List<RouteDTO> getRouteDTOs(LocalDate effectiveOnDate) {
        Set<Route> routesOnDate = routeRepository.getRoutesRunningOn(effectiveOnDate);
        List<RouteDTO> dtos = routesOnDate.stream().
                map(route -> new RouteDTO(route, getLocationsAlong(route, true))).
                collect(Collectors.toList());
        Set<String> uniqueNames = dtos.stream().map(RouteRefDTO::getRouteName).collect(Collectors.toSet());
        if (uniqueNames.size() != dtos.size()) {
            logger.warn(format("Got duplicate route names for routes %s on date %s",
                    HasId.asIds(routesOnDate), effectiveOnDate));
        }
        return dtos;
    }

    @NotNull
    private List<LocationRefWithPosition> getLocationsAlong(Route route, boolean includeNotStopping) {
        return getStationsOn(route, includeNotStopping).stream().
                map(DTOFactory::createLocationRefWithPosition).
                collect(Collectors.toList());
    }

    // use for visualisation in the front-end routes map, this is approx. since some gtfs routes actually
    // branch TODO Use query of the graph DB to get the "real" representation
    public List<Station> getStationsOn(Route route, boolean includeNotStopping) {
        Set<Trip> tripsForRoute = route.getTrips();
        Optional<Trip> maybeLongest = tripsForRoute.stream().
                max(Comparator.comparingLong(a -> a.getStopCalls().totalNumber()));

        if (maybeLongest.isEmpty()) {
            logger.error("Found no longest trip for route " + route.getId());
            return Collections.emptyList();
        }

        Trip longestTrip = maybeLongest.get();
        StopCalls stops = longestTrip.getStopCalls();
        return stops.getStationSequence(includeNotStopping);
    }

}

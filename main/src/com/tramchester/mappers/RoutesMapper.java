package com.tramchester.mappers;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.RouteDTO;
import com.tramchester.domain.presentation.DTO.LocationRefWithPosition;
import com.tramchester.domain.presentation.DTO.factory.DTOFactory;
import com.tramchester.repository.TransportData;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

@LazySingleton
public class RoutesMapper {
    private static final Logger logger = LoggerFactory.getLogger(RoutesMapper.class);

    private final Map<String, RouteDTO> routeDTOs;

    private final TransportData transportData;
    private final DTOFactory DTOFactory;

    @Inject
    public RoutesMapper(TransportData transportData, DTOFactory DTOFactory) {
        this.DTOFactory = DTOFactory;
        routeDTOs = new HashMap<>();
        this.transportData = transportData;
    }

    @PostConstruct
    private void start() {
        logger.info("Starting");
        Collection<Route> routes = transportData.getRoutes();
        routes.forEach(route -> {
            List<LocationRefWithPosition> callingStations = getLocationsAlong(route);
            String name = route.getName();
            if (routeDTOs.containsKey(name)) {
                if (!routeDTOs.get(name).getStations().equals(callingStations)) {
                    logger.warn("Mismatch on route calling stations for route " + name);
                }
            } else {
                RouteDTO routeDTO = new RouteDTO(route, callingStations);
                routeDTOs.put(name, routeDTO);
            }
        });
        logger.info("started");
    }

    @PreDestroy
    private void stop() {
        routeDTOs.clear();
    }

    public List<RouteDTO> getRouteDTOs() {
        return new ArrayList<>(routeDTOs.values());
    }

    @NotNull
    private List<LocationRefWithPosition> getLocationsAlong(Route route) {
        return getStationsOn(route).stream().
                map(DTOFactory::createLocationRefWithPosition).
                collect(Collectors.toList());
    }

    // use for visualisation in the front-end routes map, this is approx. since some gtfs routes actually
    // branch TODO Use query of the graph DB to get the "real" representation
    private List<Station> getStationsOn(Route route) {
        Set<Trip> tripsForRoute = route.getTrips();
        Optional<Trip> maybeLongest = tripsForRoute.stream().
                max(Comparator.comparingLong(a -> a.getStopCalls().totalNumber()));

        if (maybeLongest.isEmpty()) {
            logger.error("Found no longest trip for route " + route.getId());
            return Collections.emptyList();
        }

        Trip longestTrip = maybeLongest.get();
        StopCalls stops = longestTrip.getStopCalls();
        return stops.getStationSequence();
    }

}

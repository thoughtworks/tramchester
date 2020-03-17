package com.tramchester.repository;

import com.tramchester.domain.Route;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.presentation.DTO.RouteDTO;
import com.tramchester.domain.presentation.DTO.StationDTO;
import com.tramchester.domain.presentation.ProximityGroup;
import com.tramchester.resources.RouteCodeToClassMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Stream;

// TODO THIS IS A MAPPER
public class RoutesRepository {
    private static final Logger logger = LoggerFactory.getLogger(RoutesRepository.class);

    private final TransportData transportData;
    private RouteCodeToClassMapper mapper;

    public RoutesRepository(TransportData transportData, RouteCodeToClassMapper mapper) {
        this.transportData = transportData;
        this.mapper = mapper;
    }

    public List<RouteDTO> getAllRoutes() {
        List<RouteDTO> routeDTOs = new LinkedList<>();
        Collection<Route> routes = transportData.getRoutes();
        routes.forEach(route-> populateDTOFor(route, routeDTOs));
        logger.info(String.format("Found %s routes", routes.size()));
        return routeDTOs;
    }

    private void populateDTOFor(Route route, List<RouteDTO> gather) {
        Set<StationDTO> stations = new HashSet<>();
        String routeName = route.getName();
        logger.debug("Finding stations for route "  + routeName);

        Stream<Trip> trips = transportData.getTripsByRoute(route);

        trips.forEach(trip -> {
            trip.getStops().stream().forEach(stop -> {
                stations.add(new StationDTO(stop.getStation(), ProximityGroup.ALL));
            });
        });

        gather.add(new RouteDTO(routeName, route.getShortName(), new LinkedList<>(stations), mapper.map(route.getId())));
    }

}

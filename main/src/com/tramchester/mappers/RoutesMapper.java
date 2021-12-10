package com.tramchester.mappers;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.presentation.DTO.RouteDTO;
import com.tramchester.domain.presentation.DTO.StationRefWithPosition;
import com.tramchester.repository.RouteCallingStations;
import com.tramchester.repository.TransportData;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.*;

@LazySingleton
public class RoutesMapper {
    private static final Logger logger = LoggerFactory.getLogger(RoutesMapper.class);

    private final Map<String, RouteDTO> routeDTOs;

    private final TransportData transportData;
    private final RouteCallingStations routeCallingStations;

    @Inject
    public RoutesMapper(TransportData transportData, RouteCallingStations routeCallingStations) {
        routeDTOs = new HashMap<>();

        this.transportData = transportData;
        this.routeCallingStations = routeCallingStations;
    }

    @PostConstruct
    private void start() {
        logger.info("Starting");
        Collection<Route> routes = transportData.getRoutes();
        routes.forEach(route -> {
            List<StationRefWithPosition> callingStations = getRouteCallingStationRefDTO(route);
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

    public List<RouteDTO> getAllRoutes() {
        return new ArrayList<>(routeDTOs.values());
    }

    @NotNull
    private List<StationRefWithPosition> getRouteCallingStationRefDTO(Route route) {
        List<RouteCallingStations.StationWithCost> calledAtStations = routeCallingStations.getStationsFor(route);
        List<StationRefWithPosition> stationDTOs = new ArrayList<>(calledAtStations.size());
        calledAtStations.forEach(calledAtStation -> stationDTOs.add(new StationRefWithPosition(calledAtStation.getStation())));
        return stationDTOs;
    }

}

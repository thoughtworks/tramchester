package com.tramchester.mappers;

import com.tramchester.domain.Route;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.RouteDTO;
import com.tramchester.domain.presentation.DTO.StationRefWithPosition;
import com.tramchester.repository.RouteCallingStations;
import com.tramchester.repository.TransportData;
import com.tramchester.resources.RouteCodeToClassMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class RoutesMapper {
    private static final Logger logger = LoggerFactory.getLogger(RoutesMapper.class);

    private final TransportData transportData;
    private final RouteCallingStations routeCallingStations;
    private final RouteCodeToClassMapper mapper;

    public RoutesMapper(TransportData transportData, RouteCallingStations routeCallingStations, RouteCodeToClassMapper mapper) {
        this.transportData = transportData;
        this.routeCallingStations = routeCallingStations;
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
        List<Station> calledAtStations = routeCallingStations.getStationsFor(route);
        List<StationRefWithPosition> stationDTOs = new ArrayList<>(calledAtStations.size());
        calledAtStations.forEach(calledAtStation -> stationDTOs.add(new StationRefWithPosition(calledAtStation)));
        gather.add(new RouteDTO(route.getName(), route.getShortName(), stationDTOs, mapper.map(route), route.getTransportMode()));
    }

}

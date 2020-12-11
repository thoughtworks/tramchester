package com.tramchester.mappers;

import com.tramchester.domain.Route;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.RouteDTO;
import com.tramchester.domain.presentation.DTO.StationRefWithPosition;
import com.tramchester.repository.RouteCallingStations;
import com.tramchester.repository.TransportData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Singleton
public class RoutesMapper {
    private static final Logger logger = LoggerFactory.getLogger(RoutesMapper.class);

    private final TransportData transportData;
    private final RouteCallingStations routeCallingStations;

    @Inject
    public RoutesMapper(TransportData transportData, RouteCallingStations routeCallingStations) {
        this.transportData = transportData;
        this.routeCallingStations = routeCallingStations;
    }

    public List<RouteDTO> getAllRoutes() {
        List<RouteDTO> results = new ArrayList<>();
        Collection<Route> routes = transportData.getRoutes();
        routes.forEach(route-> results.add(createDTOFor(route)));

        logger.info(String.format("Found %s routes", results.size()));
        return results;
    }

    private RouteDTO createDTOFor(Route route) {
        List<Station> calledAtStations = routeCallingStations.getStationsFor(route);
        List<StationRefWithPosition> stationDTOs = new ArrayList<>(calledAtStations.size());
        calledAtStations.forEach(calledAtStation -> stationDTOs.add(new StationRefWithPosition(calledAtStation)));

        return new RouteDTO(route, stationDTOs);
    }

}

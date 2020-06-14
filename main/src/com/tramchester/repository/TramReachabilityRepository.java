package com.tramchester.repository;

import com.tramchester.domain.Route;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.RouteReachable;
import com.tramchester.mappers.RoutesMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class TramReachabilityRepository {
    private static final Logger logger = LoggerFactory.getLogger(RoutesMapper.class);

    private final RouteReachable routeReachable;
    private final TransportData transportData;

    private final List<String> tramStationIndexing; // a list as we need ordering and IndexOf
    private final Map<String, boolean[]> matrix; // stationId -> boolean[]

    public TramReachabilityRepository(RouteReachable routeReachable, TransportData transportData) {
        this.routeReachable = routeReachable;
        this.transportData = transportData;
        tramStationIndexing = new ArrayList<>();
        matrix = new HashMap<>();
    }

    public void buildRepository() {
        logger.info("Build repository");

        Set<RouteStation> routeStations = transportData.getRouteStations().stream().
                filter(RouteStation::isTram).
                collect(Collectors.toSet());
        Set<Station> tramStations = transportData.getStations().stream().
                filter(Station::isTram).
                collect(Collectors.toSet());

        tramStations.forEach(uniqueStation -> tramStationIndexing.add(uniqueStation.getId()));

        int size = tramStations.size();
        routeStations.forEach(routeStation -> {
            boolean[] flags = new boolean[size];
            String startStationId = routeStation.getStationId();
            tramStations.forEach(destinationStation -> {
                String destinationStationId = destinationStation.getId();
                boolean result;
                if (destinationStationId.equals(startStationId)) {
                    result = true;
                } else {
                    result = routeReachable.getRouteReachableWithInterchange(routeStation.getRoute(), startStationId,
                            destinationStationId);
                }
                flags[tramStationIndexing.indexOf(destinationStationId)] = result;
            });
            matrix.put(routeStation.getId(), flags);
        });
        logger.info(format("Added %s entries", size));
    }

    public boolean stationReachable(Station startStation, Route route, Station destinationStation) {
        RouteStation routeStation =  transportData.getRouteStation(RouteStation.formId(startStation, route));
        return stationReachable(routeStation, destinationStation);
    }

    public boolean stationReachable(RouteStation routeStation, Station destinationStation) {
        if (routeStation.isTram() && destinationStation.isTram()) {
            // route station is a tram station
            int index = tramStationIndexing.indexOf(destinationStation.getId());
            if (index<0) {
                throw new RuntimeException(format("Failed to find index for %s routeStation was %s", destinationStation,
                        routeStation));
            }
            return matrix.get(routeStation.getId())[index];
        }
        throw new RuntimeException("Call for trams only");
    }

}

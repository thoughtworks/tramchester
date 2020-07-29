package com.tramchester.repository;

import com.tramchester.domain.IdFor;
import com.tramchester.domain.Route;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.RouteReachable;
import com.tramchester.mappers.RoutesMapper;
import org.picocontainer.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class TramReachabilityRepository implements Disposable {
    private static final Logger logger = LoggerFactory.getLogger(RoutesMapper.class);

    private final RouteReachable routeReachable;
    private final TransportData transportData;

    private final List<IdFor<Station>> tramStationIndexing; // a list as we need ordering and IndexOf
    private final Map<IdFor<RouteStation>, boolean[]> matrix; // stationId -> boolean[]

    public TramReachabilityRepository(RouteReachable routeReachable, TransportData transportData) {
        this.routeReachable = routeReachable;
        this.transportData = transportData;
        tramStationIndexing = new ArrayList<>();
        matrix = new HashMap<>();
    }

    @Override
    public void dispose() {
        matrix.clear();
        tramStationIndexing.clear();
    }

    public void buildRepository() {
        logger.info("Build repository");

        Set<RouteStation> routeStations = transportData.getRouteStations().stream().
                filter(TransportMode::isTram).
                collect(Collectors.toSet());
        Set<Station> tramStations = transportData.getStations().stream().
                filter(TransportMode::isTram).
                collect(Collectors.toSet());

        tramStations.forEach(uniqueStation -> tramStationIndexing.add(uniqueStation.getId()));

        int size = tramStations.size();
        routeStations.forEach(routeStation -> {
            boolean[] flags = new boolean[size];
            IdFor<Station> startStationId = routeStation.getStationId();
            tramStations.forEach(destinationStation -> {
                IdFor<Station> destinationStationId = destinationStation.getId();
                boolean result;
                if (destinationStationId.equals(startStationId)) {
                    result = true;
                } else {
                    result = routeReachable.getRouteReachableWithInterchange(routeStation, destinationStation);
                }
                flags[tramStationIndexing.indexOf(destinationStationId)] = result;
            });
            matrix.put(routeStation.getId(), flags);
        });
        logger.info(format("Added %s entries", size));
    }

    // use version that takes route station
    @Deprecated
    public boolean stationReachable(Station startStation, Route route, Station destinationStation) {
        RouteStation routeStation =  transportData.getRouteStation(startStation, route);
        return stationReachable(routeStation, destinationStation);
    }

    public boolean stationReachable(RouteStation routeStation, Station destinationStation) {
        if (TransportMode.isTram(routeStation) && TransportMode.isTram(destinationStation)) {
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

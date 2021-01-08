package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.RouteReachable;
import com.tramchester.mappers.RoutesMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;


/**
* Builds a matrix representing the reachability of any tram station from a specific route station
* Used for journey planning optimisation.
*/
@LazySingleton
public class TramReachabilityRepository {
    private static final Logger logger = LoggerFactory.getLogger(TramReachabilityRepository.class);

    private final RouteReachable routeReachable;
    private final TransportData transportData;
    private final TramchesterConfig config;

    private final List<IdFor<Station>> tramStationIndexing; // a list as we need ordering and IndexOf
    private final Map<IdFor<RouteStation>, boolean[]> matrix; // stationId -> boolean[]

    @Inject
    public TramReachabilityRepository(RouteReachable routeReachable, TransportData transportData, TramchesterConfig config) {
        this.routeReachable = routeReachable;
        this.transportData = transportData;
        this.config = config;
        tramStationIndexing = new ArrayList<>();
        matrix = new HashMap<>();
    }

    @PreDestroy
    public void dispose() {
        matrix.clear();
        tramStationIndexing.clear();
    }

    @PostConstruct
    public void start() {
        buildRepository();
    }

    private void buildRepository() {
        if (!config.getTransportModes().contains(TransportMode.Tram)) {
            logger.warn("Skipping, trams not enabled");
            return;
        }

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

        String msg = format("Added %s entries", size);
        if (size>0) {
            logger.info(msg);
        } else {
            logger.warn(msg);
        }
    }

    public boolean stationReachable(RouteStation routeStation, Station destinationStation) {
        if (TransportMode.isTram(routeStation) && TransportMode.isTram(destinationStation)) {
            IdFor<Station> destinationStationId = destinationStation.getId();
            IdFor<RouteStation> routeStationId = routeStation.getId();

            return tramStationReachable(destinationStationId, routeStationId);
        }
        throw new RuntimeException("Call for trams only");
    }

    private boolean tramStationReachable(IdFor<Station> destinationStationId, IdFor<RouteStation> routeStationId) {
        int index = tramStationIndexing.indexOf(destinationStationId);
        if (index<0) {
            throw new RuntimeException(format("Failed to find index for %s routeStation was %s", destinationStationId,
                    routeStationId));
        }
        return matrix.get(routeStationId)[index];
    }


}

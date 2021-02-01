package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.RouteReachable;
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
public class ReachabilityRepository {
    private static final Logger logger = LoggerFactory.getLogger(ReachabilityRepository.class);

    private final TramchesterConfig config;
    private final TransportData transportData;
    private final RouteReachable routeReachable;
    private final Map<TransportMode, Repository> repositorys;

    @Inject
    public ReachabilityRepository(RouteReachable routeReachable, TransportData transportData, TramchesterConfig config) {
        this.routeReachable = routeReachable;
        this.transportData = transportData;
        this.config = config;
        repositorys = new HashMap<>();
    }

    @PreDestroy
    public void dispose() {
        repositorys.values().forEach(Repository::dispose);
        repositorys.clear();
    }

    @PostConstruct
    public void start() {
        logger.info("start");
        //config.getTransportModes().forEach(this::buildRepository);
        buildRepository(TransportMode.Tram);
        logger.info("started");
    }

    private void buildRepository(TransportMode mode) {

        logger.info("Building for " + mode);
        Set<RouteStation> routeStations = transportData.getRouteStations()
                .stream().
                filter(routeStation -> routeStation.getTransportModes().contains(mode)).
                collect(Collectors.toSet());

        Set<Station> destinations = transportData.getStations()
                .stream().
                filter(station -> station.getTransportModes().contains(mode)).
                collect(Collectors.toSet());


        Repository repository = new Repository();
        repositorys.put(mode, repository);

        repository.populateFor(routeStations, destinations, routeReachable);

        logger.info("Done for " + mode);
    }

    public boolean stationReachable(RouteStation routeStation, Station destinationStation) {
        TransportMode transportMode = routeStation.getRoute().getTransportMode();
        if (!destinationStation.getTransportModes().contains(transportMode)) {
            return false;
        }
        if (!repositorys.containsKey(transportMode)) {
            String msg = "Cannot find repository for " + transportMode;
            logger.error(msg);
            throw new RuntimeException(msg);
        }
        return repositorys.get(transportMode).stationReachable(destinationStation.getId(), routeStation.getId());
    }

    private static class Repository {
        private final List<StringIdFor<Station>> stationIndex; // a list as we need ordering and IndexOf
        private final Map<StringIdFor<RouteStation>, boolean[]> matrix; // stationId -> boolean[]

        private Repository() {
            stationIndex = new ArrayList<>();
            matrix = new HashMap<>();
        }

        public void dispose() {
            stationIndex.clear();
            matrix.clear();
        }

        public void populateFor(Set<RouteStation> routeStations, Set<Station> destinations, RouteReachable routeReachable) {
            logger.info(format("Build repository for %s routestations and %s stations", routeStations.size(), destinations.size()));

            destinations.forEach(uniqueStation -> stationIndex.add(uniqueStation.getId()));

            int size = destinations.size();
            routeStations.forEach(start -> {
                boolean[] flags = new boolean[size];
                StringIdFor<Station> startStationId = start.getStationId();
                destinations.forEach(destinationStation -> {
                    StringIdFor<Station> destinationStationId = destinationStation.getId();
                    boolean result;
                    if (destinationStationId.equals(startStationId)) {
                        result = true;
                    } else {
                        result = routeReachable.getRouteReachableWithInterchange(start, destinationStation);
                    }
                    flags[stationIndex.indexOf(destinationStationId)] = result;
                });
                matrix.put(start.getId(), flags);
            });

            String msg = format("Added %s entries", size);
            if (size>0) {
                logger.info(msg);
            } else {
                logger.warn(msg);
            }
        }

        private boolean stationReachable(StringIdFor<Station> destinationStationId, StringIdFor<RouteStation> routeStationId) {
            int index = stationIndex.indexOf(destinationStationId);
            if (index<0) {
                throw new RuntimeException(format("Failed to find index for %s routeStation was %s", destinationStationId,
                        routeStationId));
            }
            return matrix.get(routeStationId)[index];
        }
    }

}

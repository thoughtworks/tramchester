package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.RouteReachable;
import com.tramchester.graph.graphbuild.GraphFilter;
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

    private final TransportData transportData;
    private final TramchesterConfig config;
    private final GraphFilter graphFilter;
    private final RouteReachable routeReachable;
    private final Map<TransportMode, Repository> repositorys;

    @Inject
    public ReachabilityRepository(RouteReachable routeReachable, TransportData transportData, TramchesterConfig config, GraphFilter graphFilter) {
        this.routeReachable = routeReachable;
        this.transportData = transportData;
        this.config = config;
        this.graphFilter = graphFilter;
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
        config.getTransportModes().forEach(this::buildRepository);
        logger.info("started");
    }

    private void buildRepository(TransportMode mode) {

        logger.info("Building for " + mode);
        Set<RouteStation> routeStations = transportData.getRouteStations()
                .stream().
                filter(routeStation -> graphFilter.shouldInclude(transportData, routeStation.getRoute())).
                filter(routeStation -> routeStation.getTransportModes().contains(mode)).
                collect(Collectors.toSet());

        Repository repository = new Repository();
        repositorys.put(mode, repository);

        logger.info(format("Build repository for %s %s routestations", routeStations.size(), mode));
        repository.populateFor(routeStations, routeReachable);

        logger.info("Done for " + mode);
    }

    public boolean stationReachable(RouteStation routeStation, Station destinationStation) {
        TransportMode transportMode = routeStation.getRoute().getTransportMode();

        if (!repositorys.containsKey(transportMode)) {
            String msg = "Cannot find repository for " + transportMode;
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        return repositorys.get(transportMode).stationReachable(routeStation.getId(), destinationStation.getId());
    }

    private static class Repository {
        private final IdSet<RouteStation> canReachInterchange;
        private final Map<IdFor<RouteStation>, IdSet<Station>> reachableFrom;

        private Repository() {
            canReachInterchange = new IdSet<>();
            reachableFrom = new HashMap<>();
        }

        public void dispose() {
            canReachInterchange.clear();
            reachableFrom.clear();
        }

        public void populateFor(Set<RouteStation> startingPoints, RouteReachable routeReachable) {

            Set<RouteStation> cannotReachInterchange = new HashSet<>();

            startingPoints.forEach(start -> {
                if (routeReachable.isInterchangeReachableOnRoute(start))  {
                    canReachInterchange.add(start.getId());
                } else {
                    cannotReachInterchange.add(start);
                }
            });

            cannotReachInterchange.forEach(start -> {
                IdSet<Station> reachableStations = routeReachable.getReachableStationsOnRoute(start);
                reachableFrom.put(start.getId(), reachableStations);
            });

            long zeroReachable = reachableFrom.values().stream().filter(IdSet::isEmpty).count();
            long viaRoute = reachableFrom.size() - zeroReachable;

            logger.info(format("For %s route stations, via interchange %s, route %s, none %s",
                    startingPoints.size(), canReachInterchange.size(), viaRoute, zeroReachable));

            cannotReachInterchange.clear();
        }

        private boolean stationReachable(IdFor<RouteStation> routeStationId, IdFor<Station> destinationStationId) {
            if (canReachInterchange.contains(routeStationId)) {
                return true;
            }
            if (reachableFrom.containsKey(routeStationId)) {
                return reachableFrom.get(routeStationId).contains(destinationStationId);
            }
            return false;
        }
    }

}

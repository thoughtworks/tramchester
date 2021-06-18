package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.RouteReachable;
import com.tramchester.graph.filters.GraphFilter;
import com.tramchester.metrics.Timing;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


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
        logger.info("stopping");
        repositorys.values().forEach(Repository::dispose);
        repositorys.clear();
        logger.info("stopped");
    }

    @PostConstruct
    public void start() {
        logger.info("start");
        config.getTransportModes().forEach(this::buildRepository);
        logger.info("started");
    }

    private void buildRepository(TransportMode mode) {
        try(Timing timing = new Timing(logger, "build for " + mode)) {
            Repository repository = new Repository();
            repositorys.put(mode, repository);
            repository.populateFor(transportData, mode, graphFilter, routeReachable);
        }
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
        private IdSet<RouteStation> canReachInterchange;
        private Map<IdFor<RouteStation>, IdSet<Station>> reachableFrom;

        private Repository() {

        }

        public void dispose() {
            canReachInterchange.clear();
            reachableFrom.clear();
        }

        public void populateFor(StationRepository stationRepository, TransportMode mode, GraphFilter graphFilter, RouteReachable routeReachable) {

            logger.info("Find interchange reachable for " + mode);
            canReachInterchange = getRouteStationStream(stationRepository, mode, graphFilter).
                    parallel().
                    filter(routeReachable::isInterchangeReachableOnRoute).
                    collect(IdSet.collector());

            Stream<RouteStation> notViaInterchange = getRouteStationStream(stationRepository, mode, graphFilter).
                    filter(routeStation -> !canReachInterchange.contains(routeStation.getId()));

            logger.info("Find reachable on route for " + mode);
            reachableFrom = notViaInterchange.parallel().
                    map(routeStation -> Pair.of(routeStation.getId(), routeReachable.getReachableStationsOnRoute(routeStation))).
                    collect(Collectors.toConcurrentMap(Pair::getLeft, Pair::getRight));

        }

        @NotNull
        private Stream<RouteStation> getRouteStationStream(StationRepository stationRepository, TransportMode mode, GraphFilter graphFilter) {
            return stationRepository.getRouteStations().stream().
                    filter(routeStation -> graphFilter.shouldIncludeRoute(routeStation.getRoute())).
                    filter(routeStation -> routeStation.getTransportModes().contains(mode));
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
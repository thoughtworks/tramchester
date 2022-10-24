package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.FindRouteEndPoints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

// TODO NOW ONLY USED FOR TEST SUPPORT, MOVE

@LazySingleton
public class RouteEndRepository {
    private static final Logger logger = LoggerFactory.getLogger(RouteEndRepository.class);

    private final FindRouteEndPoints findRouteEndPoints;
    private final StationRepository stationRepository;
    private final Map<TransportMode,IdSet<Station>> beginOrEndOfRoutes;
    private final Set<TransportMode> enabledModes;

    @Inject
    public RouteEndRepository(FindRouteEndPoints findRouteEndPoints, StationRepository stationRepository, TramchesterConfig config) {
        this.findRouteEndPoints = findRouteEndPoints;
        this.stationRepository = stationRepository;
        beginOrEndOfRoutes = new HashMap<>();
        enabledModes = config.getTransportModes();
    }

    @PostConstruct
    public void start() {
        logger.info("start");
        for(TransportMode mode : enabledModes) {
            IdSet<RouteStation> starts = findRouteEndPoints.searchForStarts(mode);
            logger.info("Found " + starts.size() + " route start points");
            IdSet<RouteStation> ends = findRouteEndPoints.searchForEnds(mode);
            logger.info("Found " + ends.size() + " route end points");

            IdSet<Station> stationIds = getStationIds(starts);
            stationIds.addAll(getStationIds(ends));

            beginOrEndOfRoutes.put(mode, stationIds);
            logger.info("Found " + stationIds.size() + " ends of routes for " + mode);
        }

        logger.info("started");
    }

    @PreDestroy
    public void stop() {
        logger.info("Stop");
        beginOrEndOfRoutes.values().forEach(IdSet::clear);
        beginOrEndOfRoutes.clear();
        logger.info("stopped");
    }

    private IdSet<Station> getStationIds(IdSet<RouteStation> starts) {
        return starts.stream().
                map(stationRepository::getRouteStationById).
                map(RouteStation::getStationId).collect(IdSet.idCollector());
    }

    public IdSet<Station> getStations(TransportMode mode) {
        return beginOrEndOfRoutes.get(mode);
    }

//    public boolean isEndRoute(IdFor<Station> stationId) {
//        for (TransportMode enabledMode : enabledModes) {
//            if (beginOrEndOfRoutes.get(enabledMode).contains(stationId)) {
//                return true;
//            }
//        }
//        return false;
//    }
}

package com.tramchester.mappers;

import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.Station;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.graph.TramRouteReachable;
import com.tramchester.repository.LiveDataSource;
import com.tramchester.repository.StationAdjacenyRepository;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class TramPositionInference {
    private static final Logger logger = LoggerFactory.getLogger(TramPositionInference.class);

    private final LiveDataSource liveDataSource;
    private final StationAdjacenyRepository adjacenyRepository;
    private final TramRouteReachable routeReachable;

    public TramPositionInference(LiveDataSource liveDataSource, StationAdjacenyRepository adjacenyRepository, TramRouteReachable routeReachable) {
        this.liveDataSource = liveDataSource;
        this.adjacenyRepository = adjacenyRepository;
        this.routeReachable = routeReachable;
    }

    public Set<DueTram> findBetween(Station start, Station neighbour) {

        int cost = adjacenyRepository.getAdjacent(start, neighbour);
        if (cost<0) {
            logger.info(format("Not adjacent %s to %s", start, neighbour));
            return Collections.emptySet();
        }

        List<Route> routesBetween = routeReachable.getRoutesFromStartToNeighbour(start, neighbour.getId());

        // get departure info at neighbouring station for relevant routes
        Set<StationDepartureInfo> departureInfos = new HashSet<>();
        routesBetween.forEach(route -> {
            List<Platform> platforms = neighbour.getPlatformsForRoute(route);
            platforms.forEach(platform -> departureInfos.add(liveDataSource.departuresFor(platform)));
        });

        if (departureInfos.isEmpty()) {
            logger.warn("Unable to find departure information for " + neighbour.getPlatforms());
            return Collections.emptySet();
        }

        return departureInfos.stream().
                map(mapEntry -> mapEntry.getDueTramsWithinWindow(cost)).
                flatMap(Collection::stream).
                collect(Collectors.toSet());
    }
}

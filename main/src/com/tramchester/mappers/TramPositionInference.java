package com.tramchester.mappers;

import com.google.common.base.Functions;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.Station;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.graph.TramRouteReachable;
import com.tramchester.repository.LiveDataSource;
import io.swagger.models.auth.In;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class TramPositionInference {
    private static final Logger logger = LoggerFactory.getLogger(TramPositionInference.class);

    private final LiveDataSource liveDataSource;
    private final TramRouteReachable routeReachable;

    public TramPositionInference(LiveDataSource liveDataSource, TramRouteReachable routeReachable) {
        this.liveDataSource = liveDataSource;
        this.routeReachable = routeReachable;
    }

    public Set<DueTram> findBetween(Station first, Station second) {
        Set<Route> firstRoutes = first.getRoutes();

        // find routes that serve first to second, => pair(route, cost)
        Map<Route,Integer> routesFromFirstToSecond = firstRoutes.stream().
                map(route -> Pair.of(route, routeReachable.
                        getRouteReachableAjacent(first.getId(), second.getId(), route.getId()))).
                filter(pair -> pair.getRight()>=0).
                collect(Collectors.toMap(Pair::getLeft, Pair::getRight));

        if (routesFromFirstToSecond.isEmpty()) {
            logger.info(format("Found no routes from %s to %s", first, second));
            return Collections.emptySet();
        }

        // get the platforms at second that serve routes that link the two stations
        Map<StationDepartureInfo, Integer> departureInfos = new HashMap<>();
        second.getPlatforms().forEach(platform ->
                {
                    Set<Route> platformRoutes = platform.getRoutes();
                    platformRoutes.retainAll(routesFromFirstToSecond.keySet());
                    if (!platformRoutes.isEmpty()) {
                        Route platformRoute = platformRoutes.iterator().next();
                        int actualCost = routesFromFirstToSecond.get(platformRoute);
                        departureInfos.put(liveDataSource.departuresFor(platform), actualCost);
                    }
                }
        );

        if (departureInfos.isEmpty()) {
            logger.warn("Unable to find departure information for " + second.getPlatforms());
            return Collections.emptySet();
        }

        return departureInfos.entrySet().stream().
                map(mapEntry -> mapEntry.getKey().getDueTramsWithinWindow(mapEntry.getValue())).
                flatMap(Collection::stream).
                collect(Collectors.toSet());
    }
}

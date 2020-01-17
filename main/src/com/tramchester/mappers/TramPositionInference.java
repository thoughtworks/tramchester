package com.tramchester.mappers;

import com.google.common.base.Functions;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.Station;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.graph.TramRouteReachable;
import com.tramchester.repository.LiveDataSource;
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

        // find routes that servce first to second
        Set<Route> routesFromFirstToSecond = firstRoutes.stream().
                filter(route -> routeReachable.getRouteReachableAjacent(first.getId(), second.getId(), route.getId()))
                .collect(Collectors.toSet());

        if (routesFromFirstToSecond.isEmpty()) {
            logger.info(format("Found no routes from %s to %s", first, second));
            return Collections.emptySet();
        }

        // get the platforms at second that serve those route
        Set<StationDepartureInfo> departureInfos = new HashSet<>();
        second.getPlatforms().forEach(platform ->
                {
                    Set<Route> routes = platform.getRoutes();
                    routes.retainAll(routesFromFirstToSecond);
                    if (!routes.isEmpty()) {
                        departureInfos.add(liveDataSource.departuresFor(platform));
                    }
                }
        );

        if (departureInfos.isEmpty()) {
            logger.warn("Unable to find departure information for " + second.getPlatforms());
            return Collections.emptySet();
        }

        return departureInfos.stream().
                map(StationDepartureInfo::getDueTrams).
                flatMap(Collection::stream).
                collect(Collectors.toSet());
    }
}

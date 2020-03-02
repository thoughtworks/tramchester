package com.tramchester.livedata;

import com.tramchester.domain.*;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.RouteReachable;
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

    public static final String DEPARTING = "Departing";

    private final LiveDataSource liveDataSource;
    private final StationAdjacenyRepository adjacenyRepository;
    private final RouteReachable routeReachable;

    public TramPositionInference(LiveDataSource liveDataSource, StationAdjacenyRepository adjacenyRepository, RouteReachable routeReachable) {
        this.liveDataSource = liveDataSource;
        this.adjacenyRepository = adjacenyRepository;
        this.routeReachable = routeReachable;
    }

    // todo refresh this based on live data refresh
    public List<TramPosition> inferWholeNetwork(TramServiceDate date, TramTime time) {
        logger.info("Infer tram positions for whole network");
        List<TramPosition> results = new ArrayList<>();
        Set<Pair<Station, Station>> pairs = adjacenyRepository.getStationParis();
        pairs.forEach(pair -> {
            TramPosition result = findBetween(pair.getLeft(), pair.getRight(), date, time);
            results.add(result);
        });
        logger.info(format("Found %s pairs with trams", results.size()));
        return results;
    }

    public TramPosition findBetween(Station start, Station neighbour, TramServiceDate date, TramTime time) {
        int cost = adjacenyRepository.getAdjacent(start, neighbour);
        if (cost<0) {
            logger.info(format("Not adjacent %s to %s", start, neighbour));
            return new TramPosition(start, neighbour, Collections.emptySet(), cost);
        }

        Set<DueTram> dueTrams = getDueTrams(start, neighbour, date, time, cost);

        logger.info(format("Found %s trams between %s and %s", dueTrams.size(), start, neighbour));

        return new TramPosition(start, neighbour, dueTrams, cost);
    }

    private Set<DueTram> getDueTrams(Station start, Station neighbour, TramServiceDate date, TramTime time, int cost) {
        if (! (start.isTram() && neighbour.isTram()) ) {
            logger.info(format("Not both tram stations %s and %s", start, neighbour));
            return Collections.emptySet();
        }
        List<Route> routesBetween = routeReachable.getRoutesFromStartToNeighbour(start, neighbour.getId());

        // get departure info at neighbouring station for relevant routes
        Set<StationDepartureInfo> departureInfos = new HashSet<>();
        routesBetween.forEach(route -> {
            List<Platform> platforms = neighbour.getPlatformsForRoute(route);
            platforms.forEach(platform -> {
                liveDataSource.departuresFor(platform, date, time).ifPresent(departureInfos::add);
            });
        });

        if (departureInfos.isEmpty()) {
            logger.warn("Unable to find departure information for " + neighbour.getPlatforms());
            return Collections.emptySet();
        } else {
            return departureInfos.stream().
                    map(info -> info.getDueTramsWithinWindow(cost)).
                    flatMap(Collection::stream).
                    filter(dueTram -> !DEPARTING.equals(dueTram.getStatus())).
                    collect(Collectors.toSet());
        }

    }
}

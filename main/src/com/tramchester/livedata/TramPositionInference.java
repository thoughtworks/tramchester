package com.tramchester.livedata;

import com.tramchester.domain.*;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.liveUpdates.PlatformDueTrams;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.RouteReachable;
import com.tramchester.repository.DueTramsSource;
import com.tramchester.repository.TramStationAdjacenyRepository;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class TramPositionInference {
    private static final Logger logger = LoggerFactory.getLogger(TramPositionInference.class);

    private static final String DEPARTING = "Departing";

    private final DueTramsSource liveDataSource;
    private final TramStationAdjacenyRepository adjacenyRepository;
    private final RouteReachable routeReachable;

    public TramPositionInference(DueTramsSource liveDataSource, TramStationAdjacenyRepository adjacenyRepository, RouteReachable routeReachable) {
        this.liveDataSource = liveDataSource;
        this.adjacenyRepository = adjacenyRepository;
        this.routeReachable = routeReachable;
    }

    // todo refresh this based on live data refresh
    public List<TramPosition> inferWholeNetwork(TramServiceDate date, TramTime time) {
        logger.info("Infer tram positions for whole network");
        List<TramPosition> results = new ArrayList<>();
        Set<Pair<Station, Station>> pairs = adjacenyRepository.getTramStationParis();
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

        Set<DueTram> dueTrams = getDueTrams(start, neighbour, date.getDate(), time, cost);

        logger.info(format("Found %s trams between %s and %s", dueTrams.size(), start, neighbour));

        return new TramPosition(start, neighbour, dueTrams, cost);
    }

    private Set<DueTram> getDueTrams(Station start, Station neighbour, LocalDate date, TramTime time, int cost) {
        if (! (TransportMode.isTram(start) && TransportMode.isTram(neighbour)) ) {
            logger.info(format("Not both tram stations %s and %s", start, neighbour));
            return Collections.emptySet();
        }
        List<Route> routesBetween = routeReachable.getRoutesFromStartToNeighbour(start, neighbour);

        // get departure info at neighbouring station for relevant routes
        Set<PlatformDueTrams> platformDueTrams = new HashSet<>();
        routesBetween.forEach(route -> {
            List<Platform> platforms = neighbour.getPlatformsForRoute(route);
            platforms.forEach(platform -> liveDataSource.allTrams(platform.getId(), date, time).ifPresent(platformDueTrams::add));
        });

        if (platformDueTrams.isEmpty()) {
            logger.warn("Unable to find departure information for " + neighbour.getPlatforms());
            return Collections.emptySet();
        } else {
            return platformDueTrams.stream().
                    map(info -> info.getDueTramsWithinWindow(cost)).
                    flatMap(Collection::stream).
                    filter(dueTram -> !DEPARTING.equals(dueTram.getStatus())).
                    collect(Collectors.toSet());
        }

    }
}

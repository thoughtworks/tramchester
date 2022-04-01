package com.tramchester.livedata.tfgm;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.StationPair;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.RouteReachable;
import com.tramchester.livedata.domain.liveUpdates.PlatformDueTrams;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.repository.TramStationAdjacenyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

@LazySingleton
public class TramPositionInference {
    private static final Logger logger = LoggerFactory.getLogger(TramPositionInference.class);

    private static final String DEPARTING = "Departing";

    private final TramDepartureRepository departureRepository;
    private final TramStationAdjacenyRepository adjacenyRepository;
    private final RouteReachable routeReachable;

    @Inject
    public TramPositionInference(TramDepartureRepository departureRepository, TramStationAdjacenyRepository adjacenyRepository,
                                 RouteReachable routeReachable) {
        this.departureRepository = departureRepository;
        this.adjacenyRepository = adjacenyRepository;
        this.routeReachable = routeReachable;
    }

    // todo refresh this based on live data refresh
    public List<TramPosition> inferWholeNetwork(TramServiceDate date, TramTime time) {
        logger.info("Infer tram positions for whole network");
        Set<StationPair> pairs = adjacenyRepository.getTramStationParis();
        List<TramPosition> results = pairs.stream().
                map(pair -> findBetween(pair, date, time)).
                collect(Collectors.toList());

        logger.info(format("Found %s station pairs with trams between them", results.size()));
        return results;
    }

    public TramPosition findBetween(StationPair pair, TramServiceDate date, TramTime time) {
        Duration cost = adjacenyRepository.getAdjacent(pair);
        if (cost.isNegative()) {
            logger.warn(format("Not adjacent %s", pair));
            return new TramPosition(pair, Collections.emptySet(), cost);
        }

        Set<UpcomingDeparture> dueTrams = getDueTrams(pair, date.getDate(), time, cost);

        logger.debug(format("Found %s trams between %s", dueTrams.size(), pair));

        return new TramPosition(pair, dueTrams, cost);
    }

    private Set<UpcomingDeparture> getDueTrams(StationPair pair, LocalDate date, TramTime time, Duration cost) {
        Station neighbour = pair.getEnd();

        if (!pair.bothServeMode(TransportMode.Tram) ) {
            logger.info(format("Not both tram stations %s", pair));
            return Collections.emptySet();
        }
        List<Route> routesBetween = routeReachable.getRoutesFromStartToNeighbour(pair);

        // get departure info at neighbouring station for relevant routes
        Set<PlatformDueTrams> platformDueTrams = new HashSet<>();
        routesBetween.forEach(route -> {
            Set<Platform> platforms = neighbour.getPlatformsForRoute(route);
            platforms.forEach(platform -> departureRepository.dueTramsForPlatform(platform.getId(), date, time).ifPresent(platformDueTrams::add));
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

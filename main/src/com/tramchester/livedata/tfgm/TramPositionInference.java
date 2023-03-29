package com.tramchester.livedata.tfgm;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.StationPair;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.RouteReachable;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.repository.ClosedStationsRepository;
import com.tramchester.repository.TramStationAdjacenyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

@LazySingleton
public class TramPositionInference {
    private static final Logger logger = LoggerFactory.getLogger(TramPositionInference.class);

    private static final String DEPARTING = "Departing";

    private final TramDepartureRepository departureRepository;
    private final TramStationAdjacenyRepository adjacenyRepository;
    private final RouteReachable routeReachable;
    private final ClosedStationsRepository closedStationsRepository;

    @Inject
    public TramPositionInference(TramDepartureRepository departureRepository, TramStationAdjacenyRepository adjacenyRepository,
                                 RouteReachable routeReachable, ClosedStationsRepository closedStationsRepository) {
        this.departureRepository = departureRepository;
        this.adjacenyRepository = adjacenyRepository;
        this.routeReachable = routeReachable;
        this.closedStationsRepository = closedStationsRepository;
    }

    // todo refresh this based on live data refresh
    public List<TramPosition> inferWholeNetwork(LocalDateTime localDateTime) {

        TramDate date = TramDate.from(localDateTime);
        logger.info("Infer tram positions for whole network");
        Set<StationPair> pairs = adjacenyRepository.getTramStationParis();
        List<TramPosition> results = pairs.stream().
                filter(stationPair -> !closedStationsRepository.isClosed(stationPair.getEnd(), date) &&
                                !closedStationsRepository.isClosed(stationPair.getBegin(), date)).
                map(pair -> findBetween(pair, localDateTime)).
                filter(TramPosition::hasTrams).
                collect(Collectors.toList());

        if (results.isEmpty()) {
            logger.warn(format("Found no trams between stations for %s", localDateTime));
        } else {
            logger.info(format("Found %s station pairs with trams between them for %s", results.size(), localDateTime));
        }
        return results;
    }

    public TramPosition findBetween(StationPair pair, LocalDateTime now) {
        Duration costBetweenPair = adjacenyRepository.getAdjacent(pair);
        if (costBetweenPair.isNegative()) {
            logger.warn(format("Not adjacent %s", pair));
            return new TramPosition(pair, Collections.emptySet(), costBetweenPair);
        }

        TramTime currentTime = TramTime.ofHourMins(now.toLocalTime());
        TramDate date = TramDate.from(now);

        TramTime cutOff = currentTime.plus(costBetweenPair);
        TimeRange timeRange = TimeRange.of(currentTime, cutOff);

        Set<UpcomingDeparture> dueTrams = getDueTrams(pair, date, timeRange);

        // todo is this additional step still needed?
        Set<UpcomingDeparture> dueToday = dueTrams.stream().
                filter(departure -> departure.getDate().equals(date.toLocalDate())).
                collect(Collectors.toSet());

        logger.debug(format("Found %s trams between %s", dueToday.size(), pair));

        return new TramPosition(pair, dueToday, costBetweenPair);
    }

    private Set<UpcomingDeparture> getDueTrams(StationPair pair, TramDate date, TimeRange timeRange) {
        Station neighbour = pair.getEnd();

        if (!pair.bothServeMode(TransportMode.Tram)) {
            logger.info(format("Not both tram stations %s", pair));
            return Collections.emptySet();
        }

        List<UpcomingDeparture> neighbourDepartures = departureRepository.forStation(neighbour);

        if (neighbourDepartures.isEmpty()) {
            logger.debug("No departures from neighbour " + neighbour.getId());
            return Collections.emptySet();
        }

        List<Route> routesBetween = routeReachable.getRoutesFromStartToNeighbour(pair, date, timeRange, TransportMode.TramsOnly);

        if (routesBetween.isEmpty()) {
            logger.warn("No active routes between " + pair);
            return Collections.emptySet();
        }

        Set<Platform> platforms = routesBetween.stream().
                flatMap(route -> neighbour.getPlatformsForRoute(route).stream()).
                collect(Collectors.toSet());

        Set<UpcomingDeparture> departures = neighbourDepartures.stream().
                filter(UpcomingDeparture::hasPlatform).
                filter(departure -> platforms.contains(departure.getPlatform())).
                collect(Collectors.toSet());

        if (departures.isEmpty()) {
            logger.warn("Unable to find departure information for " + HasId.asIds(neighbour.getPlatforms()) + " from " + neighbourDepartures);
            return Collections.emptySet();
        }

        return departures.stream().
                filter(departure -> timeRange.contains(departure.getWhen())).
                filter(departure -> !DEPARTING.equals(departure.getStatus())).
                collect(Collectors.toSet());

    }

}

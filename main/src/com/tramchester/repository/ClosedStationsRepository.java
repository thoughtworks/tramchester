package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.ClosedStation;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.StationClosures;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.geo.StationLocations;
import com.tramchester.graph.filters.GraphFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@LazySingleton
public class ClosedStationsRepository {
    private static final Logger logger = LoggerFactory.getLogger(ClosedStationsRepository.class);

    private final Set<ClosedStation> closed;
    private final TramchesterConfig config;
    private final StationRepository stationRepository;
    private final StationLocations stationLocations;
    private final GraphFilter filter;

    @Inject
    public ClosedStationsRepository(TramchesterConfig config, StationRepository stationRepository, StationLocations stationLocations,
                                    GraphFilter filter) {
        this.config = config;
        this.stationRepository = stationRepository;
        this.stationLocations = stationLocations;

        this.filter = filter;
        closed = new HashSet<>();
    }

    @PostConstruct
    public void start() {
        logger.info("starting");
        config.getGTFSDataSource().forEach(source -> {
            Set<StationClosures> closures = new HashSet<>(source.getStationClosures());
            if (!closures.isEmpty()) {
                captureClosedStations(closures);
            } else {
                logger.info("No closures for " + source.getName());
            }
        });
        logger.warn("Added " + closed.size() + " stations closures");
        logger.info("Started");
    }

    private void captureClosedStations(Set<StationClosures> closures) {
        final MarginInMeters range = MarginInMeters.of(config.getNearestStopForWalkingRangeKM());

        closures.forEach(closure -> {
            DateRange dateRange = closure.getDateRange();
            boolean fullyClosed = closure.isFullyClosed();
            Set<ClosedStation> closedStations = closure.getStations().stream().
                    map(stationId -> createClosedStation(stationId, dateRange, fullyClosed, range)).
                    collect(Collectors.toSet());
            closed.addAll(closedStations);
        });
    }

    private ClosedStation createClosedStation(IdFor<Station> stationId, DateRange dateRange, boolean fullyClosed, MarginInMeters range) {
        Station station = stationRepository.getStationById(stationId);
        Set<Station> nearbyOpenStations = getNearbyStations(station, range);
        return new ClosedStation(station, dateRange, fullyClosed, nearbyOpenStations);
    }

    private Set<Station> getNearbyStations(Station station, MarginInMeters range) {

        Set<Station> withinRange = stationLocations.nearestStationsUnsorted(station, range).collect(Collectors.toSet());

        Set<Station> found = withinRange.stream().
                filter(filter::shouldInclude).
                collect(Collectors.toSet());

        logger.info("Found " + found.size() + " stations linked and within range of " + station.getId());

        return found;

    }

    @PreDestroy
    public void stop() {
        logger.info("Stopping");
        closed.clear();
        logger.info("Stopped");
    }

    public Set<ClosedStation> getFullyClosedStationsFor(TramDate date) {
        return getClosures(date, true).collect(Collectors.toSet());
    }

    private Stream<ClosedStation> getClosures(TramDate date, boolean fullyClosed) {
        return closed.stream().
                filter(closure -> closure.isFullyClosed() == fullyClosed).
                filter(closure -> closure.getDateRange().contains(date));
    }

    public Set<ClosedStation> getUpcomingClosuresFor(TramDate date) {
        return closed.stream().
                filter(closure -> date.isBefore(closure.getDateRange().getEndDate()) || date.equals(closure.getDateRange().getEndDate())).
                collect(Collectors.toSet());
    }

    public boolean hasClosuresOn(TramDate date) {
        return getClosures(date, true).findAny().isPresent() || getClosures(date, false).findAny().isPresent();
    }

    public Set<ClosedStation> getClosedStationsFor(DataSourceID sourceId) {
        return closed.stream().
                filter(closedStation -> closedStation.getStation().getDataSourceID().equals(sourceId)).
                collect(Collectors.toSet());
    }
}

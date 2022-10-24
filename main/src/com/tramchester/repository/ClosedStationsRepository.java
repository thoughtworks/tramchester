package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.ClosedStation;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.StationClosures;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ProvidesNow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@LazySingleton
public class ClosedStationsRepository {
    private static final Logger logger = LoggerFactory.getLogger(ClosedStationsRepository.class);

    private final Set<ClosedStation> closed;
    private final TramchesterConfig config;
    private final StationRepository stationRepository;

    @Inject
    public ClosedStationsRepository(TramchesterConfig config, StationRepository stationRepository) {
        this.config = config;
        this.stationRepository = stationRepository;
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
        closures.forEach(closure -> {
            DateRange dateRange = closure.getDateRange();
            boolean fullyClosed = closure.isFullyClosed();
            Set<ClosedStation> closedStations = closure.getStations().stream().
                    map(stationId -> createClosedStation(stationId, dateRange, fullyClosed)).
                    collect(Collectors.toSet());
            closed.addAll(closedStations);
        });
    }

    private ClosedStation createClosedStation(IdFor<Station> stationId, DateRange dateRange, boolean fullyClosed) {
        Station station = stationRepository.getStationById(stationId);
        return new ClosedStation(station, dateRange, fullyClosed);
    }

    @PreDestroy
    public void stop() {
        logger.info("Stopping");
        closed.clear();
        logger.info("Stopped");
    }

    public IdSet<Station> getFullyClosedStationsFor(TramDate date) {
        return getClosures(date, true).
                map(ClosedStation::getStation).
                collect(IdSet.collector());
    }

    private Stream<ClosedStation> getClosures(TramDate date, boolean fullyClosed) {
        return closed.stream().
                filter(closure -> closure.isFullyClosed() == fullyClosed).
                filter(closure -> closure.getDateRange().contains(date));
    }

    public Set<ClosedStation> getUpcomingClosuresFor(ProvidesNow providesNow) {
        TramDate date = providesNow.getTramDate();
        return closed.stream().
                filter(closure -> date.isBefore(closure.getDateRange().getEndDate()) || date.equals(closure.getDateRange().getEndDate())).
                collect(Collectors.toSet());
    }

    public boolean hasClosuresOn(TramDate date) {
        return getClosures(date, true).findAny().isPresent() || getClosures(date, false).findAny().isPresent();
    }

    public Set<ClosedStation> getClosedStationsFor(DataSourceID sourceId) {
        return closed.stream().
                filter(closedStation -> closedStation.getStation().getDataSourceID()==sourceId).
                collect(Collectors.toSet());
    }
}

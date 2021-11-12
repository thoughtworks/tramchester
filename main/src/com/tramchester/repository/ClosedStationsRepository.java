package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.StationClosure;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramServiceDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@LazySingleton
public class ClosedStationsRepository {
    private static final Logger logger = LoggerFactory.getLogger(ClosedStationsRepository.class);

    private final Set<StationClosure> closed;
    private final TramchesterConfig config;

    @Inject
    public ClosedStationsRepository(TramchesterConfig config) {
        this.config = config;
        closed = new HashSet<>();
    }

    @PostConstruct
    public void start() {
        logger.info("starting");
        config.getGTFSDataSource().forEach(source -> closed.addAll(source.getStationClosures()));
        logger.warn("Added " + closed.size() + " stations closures");
        logger.info("Started");
    }

    @PreDestroy
    public void stop() {
        logger.info("Stopping");
        closed.clear();
        logger.info("Stopped");
    }

    public IdSet<Station> getClosedStationsFor(TramServiceDate tramServiceDate) {
        LocalDate date = tramServiceDate.getDate();

        return closed.stream().
                filter(closure -> date.isAfter(closure.getBegin()) || date.isEqual(closure.getBegin()) ).
                filter(closure -> date.isBefore(closure.getEnd()) || date.isEqual(closure.getEnd())).
                flatMap(closure -> closure.getStations().stream()).
                collect(IdSet.idCollector());
    }

    public Set<StationClosure> getUpcomingClosuresFor(ProvidesNow providesNow) {
        LocalDate date = providesNow.getDate();
        return closed.stream().
                filter(closure -> date.isBefore(closure.getEnd()) || date.equals(closure.getEnd())).
                collect(Collectors.toSet());
    }
}

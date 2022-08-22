package com.tramchester.repository;

import com.google.common.collect.Sets;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.StationClosure;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ProvidesNow;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class ClosedStationsRepository {
    private static final Logger logger = LoggerFactory.getLogger(ClosedStationsRepository.class);

    private final Set<StationClosure> closed;
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

    public IdSet<Station> getClosedStationsFor(TramDate date) {
        return getClosedStationStream(date).collect(IdSet.idCollector());
    }

    @NotNull
    private Stream<IdFor<Station>> getClosedStationStream(TramDate date) {
        return closed.stream().
                filter(closure -> date.isAfter(closure.getBegin()) || date.isEqual(closure.getBegin())).
                filter(closure -> date.isBefore(closure.getEnd()) || date.isEqual(closure.getEnd())).
                flatMap(closure -> closure.getStations().stream());
    }

    public Set<StationClosure> getUpcomingClosuresFor(ProvidesNow providesNow) {
        TramDate date = providesNow.getTramDate();
        return closed.stream().
                filter(closure -> date.isBefore(closure.getEnd()) || date.equals(closure.getEnd())).
                collect(Collectors.toSet());
    }

    public Set<Route> getImpactedRoutes(TramDate date) {

        Set<Route> impacted = getClosedStationStream(date).
                map(stationRepository::getStationById).
                map(station -> Sets.union(station.getDropoffRoutes(), station.getPickupRoutes())).
                flatMap(Collection::stream).
                collect(Collectors.toSet());

        logger.info(format("The following routes impacted by closures on %s: %s",
                date, HasId.asIds(impacted)));

        return impacted;
    }

    public boolean hasClosuresOn(TramDate date) {
        return getClosedStationStream(date).findAny().isPresent();
    }
}

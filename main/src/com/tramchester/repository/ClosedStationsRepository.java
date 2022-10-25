package com.tramchester.repository;

import com.google.common.collect.Streams;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.ClosedStation;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.StationClosures;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.geo.StationLocations;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphQuery;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.filters.GraphFilter;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.graphbuild.StationsAndLinksGraphBuilder;
import com.tramchester.metrics.TimedTransaction;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@LazySingleton
public class ClosedStationsRepository {
    private static final Logger logger = LoggerFactory.getLogger(ClosedStationsRepository.class);

    private final Set<ClosedStation> closed;
    private final IdSet<Station> hasAClosure;
    private final TramchesterConfig config;
    private final StationRepository stationRepository;
    private final StationLocations stationLocations;
    private final GraphFilter filter;
    private final GraphDatabase database;
    private final GraphQuery graphQuery;

    @Inject
    public ClosedStationsRepository(TramchesterConfig config, StationRepository stationRepository, StationLocations stationLocations,
                                    GraphFilter filter, GraphDatabase database, StationsAndLinksGraphBuilder.Ready ready,
                                    GraphQuery graphQuery) {
        this.config = config;
        this.stationRepository = stationRepository;
        this.stationLocations = stationLocations;

        this.filter = filter;
        this.database = database;
        this.graphQuery = graphQuery;
        closed = new HashSet<>();
        hasAClosure = new IdSet<>();
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
        hasAClosure.add(stationId);

        Station station = stationRepository.getStationById(stationId);
        Set<Station> nearbyOpenStations = getNearbyLinkedStations(station, range);
        return new ClosedStation(station, dateRange, fullyClosed, nearbyOpenStations);
    }

    private Set<Station> getStationsLinkedTo(Station closedStation) {
        try(TimedTransaction timedTransaction = new TimedTransaction(database, logger, "find linked for " +closedStation.getId())) {
            Transaction txn = timedTransaction.transaction();
            Node stationNode = graphQuery.getStationNode(txn, closedStation);

            Iterable<Relationship> linkedRelations = stationNode.getRelationships(Direction.OUTGOING, TransportRelationshipTypes.LINKED);
            return Streams.stream(linkedRelations).
                    filter(relationship -> GraphProps.getLabelsFor(relationship.getEndNode()).contains(GraphLabel.STATION)).
                    map(relationship -> GraphProps.getStationId(relationship.getEndNode())).
                    map(stationRepository::getStationById).
                    collect(Collectors.toSet());

        }
    }

    private Set<Station> getNearbyLinkedStations(Station station, MarginInMeters range) {

        Set<Station> linked = getStationsLinkedTo(station);

        Stream<Station> withinRange = stationLocations.nearestStationsUnsorted(station, range);

        Set<Station> found = withinRange.
                filter(filter::shouldInclude).
                filter(linked::contains).
                collect(Collectors.toSet());

        logger.info("Found " + found.size() + " stations linked and within range of " + station.getId());

        return found;
    }

    @PreDestroy
    public void stop() {
        logger.info("Stopping");
        closed.clear();
        hasAClosure.clear();
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


    public boolean hasClosuresOn(TramDate date) {
        return getClosures(date, true).findAny().isPresent() || getClosures(date, false).findAny().isPresent();
    }

    public Set<ClosedStation> getClosedStationsFor(DataSourceID sourceId) {
        return closed.stream().
                filter(closedStation -> closedStation.getStation().getDataSourceID().equals(sourceId)).
                collect(Collectors.toSet());
    }

    public boolean isClosed(Location<?> location, TramDate date) {
        if (!(location instanceof Station)) {
            return false;
        }
        Station station = (Station) location;
        IdFor<Station> stationId = station.getId();

        if (!hasAClosure.contains(stationId)) {
            return false;
        }

        return closed.stream().
                filter(closedStation -> closedStation.getStationId().equals(stationId)).
                anyMatch(closedStation -> closedStation.getDateRange().contains(date));
    }

    public ClosedStation getClosedStation(Location<?> location, TramDate date) {
        Station station = (Station) location;
        if (station==null) {
            String msg = "Not a station " + location;
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        Optional<ClosedStation> maybe = closed.stream().
                filter(closedStation -> closedStation.getStationId().equals(station.getId())).
                filter(closedStation -> closedStation.getDateRange().contains(date)).
                findFirst();

        if (maybe.isEmpty()) {
            String msg = station.getId() + " is not closed on " + date;
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        return maybe.get();
    }
}

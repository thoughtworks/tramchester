package com.tramchester.graph;

import com.google.common.collect.Streams;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.ClosedStation;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.geo.StationLocationsRepository;
import com.tramchester.graph.filters.GraphFilter;
import com.tramchester.graph.graphbuild.CreateNodesAndRelationships;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.graphbuild.StationsAndLinksGraphBuilder;
import com.tramchester.mappers.Geography;
import com.tramchester.metrics.TimedTransaction;
import com.tramchester.repository.ClosedStationsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.StationsWithDiversionRepository;
import org.apache.commons.lang3.tuple.Pair;
import org.neo4j.graphdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.domain.id.HasId.asIds;
import static com.tramchester.graph.GraphPropertyKey.SOURCE_NAME_PROP;
import static com.tramchester.graph.TransportRelationshipTypes.DIVERSION;
import static java.lang.String.format;

@LazySingleton
public class AddWalksForClosedGraphBuilder extends CreateNodesAndRelationships implements StationsWithDiversionRepository {
    private static final Logger logger = LoggerFactory.getLogger(AddWalksForClosedGraphBuilder.class);

    private final GraphDatabase database;
    private final StationRepository stationRepository;
    private final ClosedStationsRepository closedStationsRepository;
    private final GraphQuery graphQuery;
    private final StationLocationsRepository stationLocations;
    private final TramchesterConfig config;
    private final GraphFilter filter;
    private final Geography geography;
    private final StationsWithDiversions stationsWithDiversions;

    @Inject
    public AddWalksForClosedGraphBuilder(GraphDatabase database, GraphFilter filter, GraphQuery graphQuery,
                                         StationRepository repository, ClosedStationsRepository closedStationsRepository,
                                         TramchesterConfig config, StationsAndLinksGraphBuilder.Ready ready,
                                         StationLocationsRepository stationLocations, Geography geography) {
        super(database);
        this.database = database;
        this.filter = filter;
        this.graphQuery = graphQuery;
        this.stationRepository = repository;
        this.closedStationsRepository = closedStationsRepository;
        this.config = config;

        this.stationLocations = stationLocations;
        this.geography = geography;

        stationsWithDiversions = new StationsWithDiversions();
    }

    @PostConstruct
    public void start() {

        logger.info("starting");

        config.getGTFSDataSource().forEach(source -> {
            boolean hasDBFlag = hasDBFlag(source);

            final String sourceName = source.getName();
            if (!source.getAddWalksForClosed()) {
                logger.info("Create walks is disabled in configuration for " + sourceName);
                if (hasDBFlag) {
                    final String message = "DB rebuild is required, mismatch on config (false) and db (true) for " + sourceName;
                    logger.error(message);
                    throw new RuntimeException(message);
                }
                return;
            }
            // else enabled for this source

            if (config.getMaxWalkingConnections()==0) {
                final String msg = "Max walking connections set to zero, creating walks for neighbours makes no sense";
                logger.error(msg);
                throw new RuntimeException(msg);
            }

            if (hasDBFlag) {
                logger.info("Node and prop present, assuming walks for closed already built in DB for " + sourceName);
                return;
            }

            createWalksForClosed(source);
            addDBFlag(source);
            reportStats();
        });
        logger.info("started");
    }

    @PreDestroy
    public void stop() {
        stationsWithDiversions.close();
    }

    public Ready getReady() {
        return new Ready();
    }

    private void createWalksForClosed(GTFSSourceConfig source) {
        logger.info("Add walks for closed stations for " + source.getName());
        final Set<ClosedStation> closures = closedStationsRepository.getClosedStationsFor(source.getDataSourceId());
        final IdSet<Station> closedStationIds = closures.stream().map(ClosedStation::getStationId).collect(IdSet.idCollector());

        if (closures.isEmpty()) {
            logger.warn("No station closures are given for " + source.getName());
            return;
        }

        final MarginInMeters range = MarginInMeters.of(config.getNearestStopForWalkingRangeKM());

        closures.stream().
                filter(closedStation -> filter.shouldInclude(closedStation.getStation())).
                filter(closedStation -> closedStation.getStation().getGridPosition().isValid()).
            forEach(closedStation -> {

                Set<Station> linkedTo = getStationsLinkedTo(closedStation.getStation());

                // TODO This excludes all stations with closures, more sofisicated would be to ID how/if closure
                // dates overlaps and only exclude if really necessary
                Set<Station> nearbyOpenStations = closedStation.getNearbyOpenStations().stream().
                        filter(nearby -> !closedStationIds.contains(nearby.getId())).
                        filter(linkedTo::contains).
                        collect(Collectors.toSet());

                createWalks(nearbyOpenStations, closedStation, range);

            });
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

    private void createWalks(Set<Station> linkedNearby, ClosedStation closedStation, MarginInMeters range) {
        try(TimedTransaction timedTransaction = new TimedTransaction(database, logger, "create diversions for " +closedStation.getStationId())) {
            Transaction txn = timedTransaction.transaction();
            addDiversionsToAndFromClosed(txn, linkedNearby, closedStation);
            int added = addDiversionsAroundClosed(txn, linkedNearby, closedStation, range);
            if (added==0) {
                logger.warn("Did not create any diversions around closure of " + closedStation.getStationId());
            }
            timedTransaction.commit();
        }
    }

    private boolean hasDBFlag(GTFSSourceConfig sourceConfig) {
        logger.info("Checking DB if walks added for " + sourceConfig.getName() +  " closed stations");
        boolean flag;
        try (Transaction txn = graphDatabase.beginTx()) {
            flag = graphQuery.hasAnyNodesWithLabelAndId(txn, GraphLabel.WALK_FOR_CLOSED_ENABLED,
                    SOURCE_NAME_PROP.getText(), sourceConfig.getName());
        }
        return flag;
    }

    private void addDBFlag(GTFSSourceConfig sourceConfig) {
        try (Transaction txn = graphDatabase.beginTx()) {
            ResourceIterator<Node> query = txn.findNodes(GraphLabel.WALK_FOR_CLOSED_ENABLED);
            List<Node> nodes = query.stream().collect(Collectors.toList());

            Node node;
            if (nodes.isEmpty()) {
                logger.info("Creating " + GraphLabel.WALK_FOR_CLOSED_ENABLED + " node");
                node = createGraphNode(txn, GraphLabel.WALK_FOR_CLOSED_ENABLED);
            } else {
                if (nodes.size() != 1) {
                    final String message = "Found too many " + GraphLabel.WALK_FOR_CLOSED_ENABLED + " nodes, should be one only";
                    logger.error(message);
                    throw new RuntimeException(message);
                }
                logger.info("Found " + GraphLabel.WALK_FOR_CLOSED_ENABLED + " node");
                node = nodes.get(0);
            }
            node.setProperty(SOURCE_NAME_PROP.getText(), sourceConfig.getName());

            txn.commit();
        }
    }

    private void addDiversionsToAndFromClosed(Transaction txn, Set<Station> others, ClosedStation closure) {
        Station closedStation = closure.getStation();

        Node closedNode = graphQuery.getStationNode(txn, closedStation);
        if (closedNode==null) {
            String msg = "Could not find database node for from: " + closedStation.getId();
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        logger.info("Adding Diversion relations to/from closed " + closedStation.getId() + " to/from " + asIds(others));

        others.stream().filter(filter::shouldInclude).forEach(otherStation -> {

            Duration cost = geography.getWalkingDuration(closedStation, otherStation);

            logger.info(format("Create diversion to/from %s and %s cost %s", closedStation.getId(), otherStation.getId(), cost));

            Node otherNode = graphQuery.getStationNode(txn, otherStation);
            if (otherNode==null) {
                String msg = "Could not find database node for to: " + otherStation.getId();
                logger.error(msg);
                throw new RuntimeException(msg);
            }

            Relationship fromClosed = createRelationship(closedNode, otherNode, DIVERSION);
            Relationship fromOther = createRelationship(otherNode, closedNode, DIVERSION);

            setCommonProperties(fromClosed, cost, closure);
            setCommonProperties(fromOther, cost, closure);

            GraphProps.setProperty(fromClosed, otherStation);
            GraphProps.setProperty(fromOther, closedStation);

            stationsWithDiversions.add(otherStation, closure.getDateRange());

        });
    }

    private int addDiversionsAroundClosed(Transaction txn, Set<Station> nearbyStations, ClosedStation closure, MarginInMeters range) {
        Set<Pair<Station, Station>> toLinkViaDiversion = nearbyStations.stream().
                flatMap(nearbyA -> nearbyStations.stream().map(nearbyB -> Pair.of(nearbyA, nearbyB))).
                filter(pair -> !pair.getLeft().equals(pair.getRight())).
                filter(pair -> range.within(geography.getDistanceBetweenInMeters(pair.getLeft(), pair.getRight()))).
                collect(Collectors.toSet());

        if (toLinkViaDiversion.isEmpty()) {
            return 0;
        }

        logger.info("Create " + toLinkViaDiversion.size() + " diversions to/from stations");

        Set<Station> uniqueStations = new HashSet<>();

        toLinkViaDiversion.forEach(pair -> {
            Station first = pair.getLeft();
            Station second = pair.getRight();

            uniqueStations.add(first);
            uniqueStations.add(second);

            Duration cost = geography.getWalkingDuration(first, second);

            logger.info(format("Create diversion between %s and %s cost %s", first.getId(), second.getId(), cost));

            Node firstNode = graphQuery.getStationNode(txn, first);
            Node secondNode = graphQuery.getStationNode(txn, second);

            Relationship relationship = createRelationship(firstNode, secondNode, DIVERSION);
            setCommonProperties(relationship, cost, closure);
            GraphProps.setProperty(relationship, second);
        });

        uniqueStations.forEach(station -> stationsWithDiversions.add(station, closure.getDateRange()));

        return toLinkViaDiversion.size();
    }

    private void setCommonProperties(Relationship relationship, Duration cost, ClosedStation closure) {
        GraphProps.setCostProp(relationship, cost);
        GraphProps.setStartDate(relationship, closure.getDateRange().getStartDate());
        GraphProps.setEndDate(relationship, closure.getDateRange().getEndDate());
    }

    @Override
    public boolean hasDiversions(Station station) {
        return stationsWithDiversions.hasDiversions(station);
    }

    @Override
    public Set<DateRange> getDateRangesFor(Station station) {
        return stationsWithDiversions.getDateRangesFor(station);
    }

    public static class Ready {
        private Ready() {

        }
    }

    private static class StationsWithDiversions implements StationsWithDiversionRepository {

        private final Map<Station, Set<DateRange>> closures;

        private StationsWithDiversions() {
            closures = new HashMap<>();
        }

        @Override
        public boolean hasDiversions(Station station) {
            return closures.containsKey(station);
        }

        @Override
        public Set<DateRange> getDateRangesFor(Station station) {
            return closures.get(station);
        }

        public void add(Station station, DateRange dateRange) {
            if (!closures.containsKey(station)) {
                closures.put(station, new HashSet<>());
            }
            closures.get(station).add(dateRange);
        }

        public void close() {
            closures.clear();
        }
    }
}

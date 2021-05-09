package com.tramchester.graph;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.ModeIdsMap;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.StationLocationsRepository;
import com.tramchester.graph.filters.GraphFilter;
import com.tramchester.graph.graphbuild.CreateNodesAndRelationships;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.graphbuild.StationsAndLinksGraphBuilder;
import com.tramchester.metrics.TimedTransaction;
import com.tramchester.repository.CompositeStationRepository;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class CreateNeighbours extends CreateNodesAndRelationships {
    private static final Logger logger = LoggerFactory.getLogger(CreateNeighbours.class);

    private final ModeIdsMap<Station> stationsWithNeighbours;
    private final GraphDatabase database;
    private final CompositeStationRepository repository;
    private final StationLocationsRepository stationLocations;
    private final GraphQuery graphQuery;
    private final TramchesterConfig config;
    private final double rangeInKM;
    private final GraphFilter filter;

    // ONLY link stations of different types
    private static final boolean DIFF_MODES_ONLY = true;

    ///
    // finds stations within a specified distance and adds a direct relationship between them
    // this aids with routing where stops are very close together but are not linked in any other way
    ///

    // TODO Generalise to TransportRelationshipTypes.NEIGHBOUR with TransportMode property??

    @Inject
    public CreateNeighbours(GraphDatabase database, GraphFilter filter, GraphQuery graphQuery, CompositeStationRepository repository,
                            StationLocationsRepository stationLocations, TramchesterConfig config,
                            StationsAndLinksGraphBuilder.Ready ready) {
        super(database);
        this.database = database;
        this.filter = filter;
        this.graphQuery = graphQuery;
        this.repository = repository;
        this.stationLocations = stationLocations;
        this.config = config;
        this.rangeInKM = config.getDistanceToNeighboursKM();

        stationsWithNeighbours = new ModeIdsMap<>();
    }

    @PostConstruct
    public void start() {
        boolean hasDBFlag = hasDBFlag();

        if (!config.getCreateNeighbours()) {
            logger.warn("Create neighbours is disabled in configuration");
            if (hasDBFlag) {
                throw new RuntimeException("DB rebuild is required, mismatch on config and db");
            }
            return;
        }

        if (hasDBFlag) {
            logger.warn("Node NEIGHBOURS_ENABLED present, assuming neighbours already built in DB");
            config.getTransportModes().forEach(this::populateStationsWithNeighbours);
            logger.info("Added " + stationsWithNeighbours.size() + " stations with neighbours");
            return;
        }

        logger.info("starting");
        config.getTransportModes().forEach(this::createNeighboursFor);
        addDBFlag();
        reportStats();
        logger.info("Added " + stationsWithNeighbours.size() + " stations with neighbours");
        logger.info("started");
    }

    private void populateStationsWithNeighbours(TransportMode mode) {
        logger.info("Populating stations with neighbours from existing DB");
        String stationLabel = GraphBuilder.Labels.forMode(mode).name();

        String query = format("MATCH (a:%s)-[r:NEIGHBOUR]->(b) " +
                        "WHERE NOT b:%s " +
                        "RETURN a",
                stationLabel, stationLabel);

        try (TimedTransaction timedTransaction = new TimedTransaction(graphDatabase, logger, "Stations with Neighbours " + mode) ) {
            Transaction txn = timedTransaction.transaction();
            Result result = txn.execute(query);
            while (result.hasNext()) {
                Map<String, Object> row = result.next();
                Node node = (Node) row.get("a");
                stationsWithNeighbours.add(mode, GraphProps.getStationId(node));
            }
            result.close();
        }
        logger.info("Added " + stationsWithNeighbours.sizeFor(mode) + " staions with neighbours for " + mode);
    }

    private void createNeighboursFor(TransportMode mode) {
        Set<Station> allStationsForMode = repository.getStationsForMode(mode);
        logger.info(format("Adding neighbouring stations for %s stations and range %s KM", allStationsForMode.size(), rangeInKM));

        try(TimedTransaction timedTransaction = new TimedTransaction(database, logger, "create neighbours for " + mode)) {
            Transaction txn = timedTransaction.transaction();
                allStationsForMode.stream().
                        filter(station -> station.getGridPosition().isValid()).
                        filter(filter::shouldInclude).forEach(station -> {
                    // nearby could be any transport mode
                    Stream<Station> nearby = stationLocations.
                            nearestStationsUnsorted(station, rangeInKM)
                            .filter(found -> !found.equals(station))
                            .filter(found -> DIFF_MODES_ONLY && !found.getTransportModes().contains(mode));
                    if(addNeighbourRelationships(txn, filter, station, nearby)) {
                        stationsWithNeighbours.add(mode, station.getId());
                    }
                });
            timedTransaction.commit();
        }
    }

    private boolean hasDBFlag() {
        boolean flag;
        try (Transaction txn = graphDatabase.beginTx()) {
            flag = graphQuery.hasAnyNodesWithLabel(txn, GraphBuilder.Labels.NEIGHBOURS_ENABLED);
        }
        return flag;
    }

    private void addDBFlag() {
        try (Transaction txn = graphDatabase.beginTx()) {
            txn.createNode(GraphBuilder.Labels.NEIGHBOURS_ENABLED);
            txn.commit();
        }
    }

    private boolean addNeighbourRelationships(Transaction txn, GraphFilter filter, Station from, Stream<Station> others) {
        AtomicBoolean addedAny = new AtomicBoolean(false);
        Node fromNode = graphQuery.getStationOrGrouped(txn, from);
        if (fromNode==null) {
            String msg = "Could not find database node for from: " + from.getId();
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        double mph = config.getWalkingMPH();
        logger.debug("Adding neighbour relations from " + from.getId());
        others.filter(filter::shouldInclude).forEach(to -> {

            Node toNode = graphQuery.getStationOrGrouped(txn, to);
            if (toNode==null) {
                String msg = "Could not find database node for to: " + to.getId();
                logger.error(msg);
                throw new RuntimeException(msg);
            }

            int cost = CoordinateTransforms.calcCostInMinutes(from, to, mph);
            if (addNeighbourRelationship(fromNode, toNode, cost)) {
                addedAny.getAndSet(true);
            } else {
                logger.warn(format("Already neighbour link from %s to %s", from.getId(), to.getId()));
            }
        });
        return addedAny.get();
    }

    public IdSet<Station> getStationsWithNeighbours(TransportMode mode) {
        return stationsWithNeighbours.get(mode);
    }
}

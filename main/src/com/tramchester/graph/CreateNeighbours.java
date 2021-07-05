package com.tramchester.graph;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.StationLink;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.geo.StationLocationsRepository;
import com.tramchester.graph.filters.GraphFilter;
import com.tramchester.graph.graphbuild.*;
import com.tramchester.metrics.TimedTransaction;
import com.tramchester.repository.NeighboursRepository;
import com.tramchester.repository.StationRepository;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class CreateNeighbours extends CreateNodesAndRelationships implements NeighboursRepository {
    private static final Logger logger = LoggerFactory.getLogger(CreateNeighbours.class);

    private final Map<IdFor<Station>, IdSet<Station>> neighbours;
    private final GraphDatabase database;
    private final StationRepository stationRepository;
    private final StationLocationsRepository stationLocations;
    private final GraphQuery graphQuery;
    private final TramchesterConfig config;
    private final MarginInMeters marginInMeters;
    private final GraphFilter filter;

    // ONLY link stations of different types
    private static final boolean DIFF_MODES_ONLY = true;

    ///
    // finds stations within a specified distance and adds a direct relationship between them
    // this aids with routing where stops are very close together but are not linked in any other way
    ///

    // TODO Generalise to TransportRelationshipTypes.NEIGHBOUR with TransportMode property??

    @Inject
    public CreateNeighbours(GraphDatabase database, GraphFilter filter, GraphQuery graphQuery, StationRepository repository,
                            StationLocationsRepository stationLocations, TramchesterConfig config,
                            StationsAndLinksGraphBuilder.Ready ready) {
        super(database);
        this.database = database;
        this.filter = filter;
        this.graphQuery = graphQuery;
        this.stationRepository = repository;
        this.stationLocations = stationLocations;
        this.config = config;
        this.marginInMeters = MarginInMeters.of(config.getDistanceToNeighboursKM());

        neighbours = new HashMap<>();
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
            logger.info("Node NEIGHBOURS_ENABLED present, assuming neighbours already built in DB");
            config.getTransportModes().forEach(this::loadStationsWithNeighbours);
            logger.info("Added " + neighbours.size() + " stations with neighbours");
            return;
        }

        logger.info("starting");
        config.getTransportModes().forEach(this::createNeighboursFor);
        addDBFlag();
        reportStats();
        logger.info("Added " + neighbours.size() + " stations with neighbours");
        logger.info("started");
    }

    private void loadStationsWithNeighbours(TransportMode mode) {
        logger.info("Populating stations with neighbours from existing DB");
        String stationLabel = GraphLabel.forMode(mode).name();

        String query = format("MATCH (a:%s)-[r:NEIGHBOUR]->(b) " +
                        "WHERE NOT b:%s " +
                        "RETURN a,b",
                stationLabel, stationLabel);

        int added = 0;

        try (TimedTransaction timedTransaction = new TimedTransaction(graphDatabase, logger, "Stations with Neighbours " + mode) ) {
            Transaction txn = timedTransaction.transaction();
            Result result = txn.execute(query);
            while (result.hasNext()) {
                Map<String, Object> row = result.next();
                Node startNode = (Node) row.get("a");
                Node endNode = (Node) row.get("b");
                IdFor<Station> start = GraphProps.getStationId(startNode);
                IdFor<Station> end = GraphProps.getStationId(endNode);
                if (!neighbours.containsKey(start)) {
                    neighbours.put(start, new IdSet<>());
                }
                neighbours.get(start).add(end);

                added = added + 1;
            }
            result.close();
        }
        logger.info("Added " + added + " staions with neighbours for " + mode);
    }

    private void createNeighboursFor(TransportMode mode) {
        Set<Station> allStationsForMode = stationRepository.getStationsForMode(mode);
        logger.info(format("Adding neighbouring stations for %s stations and range %s", allStationsForMode.size(), marginInMeters));

        try(TimedTransaction timedTransaction = new TimedTransaction(database, logger, "create neighbours for " + mode)) {
            Transaction txn = timedTransaction.transaction();
                allStationsForMode.stream().
                        filter(station -> station.getGridPosition().isValid()).
                        filter(filter::shouldInclude).
                        forEach(station -> {
                        // nearby could be any transport mode
                        Stream<Station> nearby = stationLocations.nearestStationsUnsorted(station, marginInMeters)
                                .filter(found -> !found.equals(station))
                                .filter(found -> DIFF_MODES_ONLY && !found.serves(mode));
                        neighbours.put(station.getId(), new IdSet<>());
                        addNeighbourRelationships(txn, filter, station, nearby);
                });
            timedTransaction.commit();
        }
    }

    private boolean hasDBFlag() {
        boolean flag;
        try (Transaction txn = graphDatabase.beginTx()) {
            flag = graphQuery.hasAnyNodesWithLabel(txn, GraphLabel.NEIGHBOURS_ENABLED);
        }
        return flag;
    }

    private void addDBFlag() {
        try (Transaction txn = graphDatabase.beginTx()) {
            txn.createNode(GraphLabel.NEIGHBOURS_ENABLED);
            txn.commit();
        }
    }

    private void addNeighbourRelationships(Transaction txn, GraphFilter filter, Station from, Stream<Station> others) {
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
                //neighbours.put(from, to);
                neighbours.get(from.getId()).add(to.getId());
            } else {
                logger.warn(format("Already neighbour link from %s to %s", from.getId(), to.getId()));
            }
        });
    }

    @Override
    public IdSet<Station> getStationsWithNeighbours(TransportMode mode) {
        return neighbours.keySet().stream().
                map(stationRepository::getStationById).
                filter(station -> station.serves(mode)).
                collect(IdSet.collector());
    }

    @Override
    public List<StationLink> getAll() {
        final Set<TransportMode> walk = Collections.singleton(TransportMode.Walk);
        return neighbours.entrySet().stream().
                flatMap(entry -> createLinks(entry.getKey(), entry.getValue(), walk)).
                collect(Collectors.toList());
    }

    @Override
    public IdSet<Station> getNeighboursFor(IdFor<Station> id) {
        return neighbours.get(id);
    }

    @Override
    public boolean differentModesOnly() {
        return DIFF_MODES_ONLY;
    }

    private Stream<StationLink> createLinks(IdFor<Station> from, IdSet<Station> others, Set<TransportMode> modes) {
        Station fromStation = stationRepository.getStationById(from);
        return others.stream().
                map(stationRepository::getStationById).
                map(other -> new StationLink(fromStation, other, modes));
    }

}

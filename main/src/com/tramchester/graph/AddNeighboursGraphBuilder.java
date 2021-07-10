package com.tramchester.graph;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.Station;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.graph.filters.GraphFilter;
import com.tramchester.graph.graphbuild.CreateNodesAndRelationships;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.graphbuild.StationsAndLinksGraphBuilder;
import com.tramchester.metrics.TimedTransaction;
import com.tramchester.repository.NeighboursRepository;
import com.tramchester.repository.StationRepository;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Set;

import static java.lang.String.format;

@LazySingleton
public class AddNeighboursGraphBuilder extends CreateNodesAndRelationships {
    private static final Logger logger = LoggerFactory.getLogger(AddNeighboursGraphBuilder.class);

    private final GraphDatabase database;
    private final StationRepository stationRepository;
    private final NeighboursRepository neighboursRepository;
    private final GraphQuery graphQuery;
    private final TramchesterConfig config;
    private final GraphFilter filter;

    @Inject
    public AddNeighboursGraphBuilder(GraphDatabase database, GraphFilter filter, GraphQuery graphQuery, StationRepository repository,
                                     TramchesterConfig config, StationsAndLinksGraphBuilder.Ready ready,
                                     NeighboursRepository neighboursRepository) {
        super(database);
        this.database = database;
        this.filter = filter;
        this.graphQuery = graphQuery;
        this.stationRepository = repository;
        this.config = config;
        this.neighboursRepository = neighboursRepository;

    }

    @PostConstruct
    public void start() {

        boolean hasDBFlag = hasDBFlag();

        if (!config.getCreateNeighbours()) {
            logger.info("Create neighbours is disabled in configuration");
            if (hasDBFlag) {
                final String message = "DB rebuild is required, mismatch on config (false) and db (true)";
                logger.error(message);
                throw new RuntimeException(message);
            }
            return;
        }

        if (hasDBFlag) {
            logger.info("Node NEIGHBOURS_ENABLED present, assuming neighbours already built in DB");
            return;
        }

        logger.info("starting");
        createNeighboursInDB();
        addDBFlag();
        reportStats();
        logger.info("started");
    }

    public Ready getReady() {
        return new Ready();
    }

    private void createNeighboursInDB() {
        try(TimedTransaction timedTransaction = new TimedTransaction(database, logger, "create neighbours")) {
            Transaction txn = timedTransaction.transaction();
                stationRepository.getStationStream().
                    filter(filter::shouldInclude).
                    filter(station -> neighboursRepository.hasNeighbours(station.getId())).
                    forEach(station -> {
                        Set<Station> neighbours = neighboursRepository.getNeighboursFor(station.getId());
                        addNeighbourRelationships(txn, filter, station, neighbours);
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

    private void addNeighbourRelationships(Transaction txn, GraphFilter filter, Station from, Set<Station> others) {
        Node fromNode = graphQuery.getStationOrGrouped(txn, from);
        if (fromNode==null) {
            String msg = "Could not find database node for from: " + from.getId();
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        double mph = config.getWalkingMPH();
        logger.debug("Adding neighbour relations from " + from.getId());
        others.stream().filter(filter::shouldInclude).forEach(to -> {

            Node toNode = graphQuery.getStationOrGrouped(txn, to);
            if (toNode==null) {
                String msg = "Could not find database node for to: " + to.getId();
                logger.error(msg);
                throw new RuntimeException(msg);
            }

            int cost = CoordinateTransforms.calcCostInMinutes(from, to, mph);
            if (!addNeighbourRelationship(fromNode, toNode, cost)) {
                logger.warn(format("Already neighbour link from %s to %s", from.getId(), to.getId()));
            }
        });
    }

    public class Ready {
        private Ready() {

        }
    }


}

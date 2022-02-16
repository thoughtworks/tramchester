package com.tramchester.graph;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.NeighbourConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.StationLink;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.databaseManagement.GraphDatabaseMetaInfo;
import com.tramchester.graph.filters.GraphFilter;
import com.tramchester.graph.graphbuild.CreateNodesAndRelationships;
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
import java.time.Duration;
import java.util.Set;

import static java.lang.String.format;

@LazySingleton
public class AddNeighboursGraphBuilder extends CreateNodesAndRelationships {
    private static final Logger logger = LoggerFactory.getLogger(AddNeighboursGraphBuilder.class);

    private final GraphDatabase database;
    private final GraphDatabaseMetaInfo databaseMetaInfo;
    private final StationRepository stationRepository;
    private final NeighboursRepository neighboursRepository;
    private final GraphQuery graphQuery;
    private final TramchesterConfig config;
    private final GraphFilter filter;

    @Inject
    public AddNeighboursGraphBuilder(GraphDatabase database, GraphDatabaseMetaInfo databaseMetaInfo, GraphFilter filter, GraphQuery graphQuery, StationRepository repository,
                                     TramchesterConfig config, StationsAndLinksGraphBuilder.Ready ready,
                                     NeighboursRepository neighboursRepository) {
        super(database);
        this.database = database;
        this.databaseMetaInfo = databaseMetaInfo;
        this.filter = filter;
        this.graphQuery = graphQuery;
        this.stationRepository = repository;
        this.config = config;
        this.neighboursRepository = neighboursRepository;

    }

    @PostConstruct
    public void start() {

        boolean hasDBFlag = hasDBFlag();

        if (!config.hasNeighbourConfig()) {
            logger.info("Create neighbours is disabled in configuration");
            if (hasDBFlag) {
                final String message = "DB rebuild is required, mismatch on config (false) and db (true)";
                logger.error(message);
                throw new RuntimeException(message);
            }
            return;
        }

        NeighbourConfig neighbourConfig = config.getNeighbourConfig();

        if (neighbourConfig.getMaxNeighbourConnections()==0) {
            String msg = "createNeighbours is true but maxNeighbourConnections==0";
            logger.error(msg);
            throw new RuntimeException(msg);
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
                stationRepository.getActiveStationStream().
                    filter(filter::shouldInclude).
                    filter(station -> neighboursRepository.hasNeighbours(station.getId())).
                    forEach(station -> {
                        Set<StationLink> links = neighboursRepository.getNeighbourLinksFor(station.getId());
                        addNeighbourRelationships(txn, filter, station, links);
                });
            timedTransaction.commit();
        }
    }

    private boolean hasDBFlag() {
        boolean flag;
        try (Transaction txn = graphDatabase.beginTx()) {
            flag = databaseMetaInfo.isNeighboursEnabled(txn);
        }
        return flag;
    }

    private void addDBFlag() {
        try (Transaction txn = graphDatabase.beginTx()) {
            databaseMetaInfo.setNeighboursEnabled(txn);
            txn.commit();
        }
    }

    private void addNeighbourRelationships(Transaction txn, GraphFilter filter, Station from, Set<StationLink> links) {
        Node fromNode = graphQuery.getStationNode(txn, from);
        if (fromNode==null) {
            String msg = "Could not find database node for from: " + from.getId();
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        logger.debug("Adding neighbour relations from " + from.getId());

        links.stream().
                filter(link -> filter.shouldInclude(link.getEnd())).
                forEach(link -> {

                Node toNode = graphQuery.getStationNode(txn, link.getEnd());
                if (toNode==null) {
                    String msg = "Could not find database node for to: " + link.getEnd().getId();
                    logger.error(msg);
                    throw new RuntimeException(msg);
                }

                Duration walkingCost = link.getWalkingTime();
                if (!addNeighbourRelationship(fromNode, toNode, walkingCost)) {
                    logger.warn(format("Already neighbour link between %s", link));
                }
            });
    }

    public class Ready {
        private Ready() {

        }
    }


}

package com.tramchester.graph;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.StationClosure;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.geo.StationLocations;
import com.tramchester.graph.filters.GraphFilter;
import com.tramchester.graph.graphbuild.CreateNodesAndRelationships;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.graphbuild.StationsAndLinksGraphBuilder;
import com.tramchester.metrics.TimedTransaction;
import com.tramchester.repository.StationRepository;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.domain.id.HasId.asIds;
import static com.tramchester.graph.GraphPropertyKey.SOURCE_NAME_PROP;
import static com.tramchester.graph.TransportRelationshipTypes.WALKS_FROM;
import static com.tramchester.graph.TransportRelationshipTypes.WALKS_TO;

@LazySingleton
public class AddWalksForClosedGraphBuilder extends CreateNodesAndRelationships {
    private static final Logger logger = LoggerFactory.getLogger(AddWalksForClosedGraphBuilder.class);

    private final GraphDatabase database;
    private final StationRepository stationRepository;
    private final GraphQuery graphQuery;
    private final StationLocations stationLocations;
    private final TramchesterConfig config;
    private final GraphFilter filter;

    @Inject
    public AddWalksForClosedGraphBuilder(GraphDatabase database, GraphFilter filter, GraphQuery graphQuery, StationRepository repository,
                                         TramchesterConfig config, StationsAndLinksGraphBuilder.Ready ready,
                                         StationLocations stationLocations) {
        super(database);
        this.database = database;
        this.filter = filter;
        this.graphQuery = graphQuery;
        this.stationRepository = repository;
        this.config = config;

        this.stationLocations = stationLocations;
    }

    @PostConstruct
    public void start() {

        logger.info("starting");

        config.getGTFSDataSource().forEach(source -> {
            boolean hasDBFlag = hasDBFlag(source);

            if (!source.getAddWalksForClosed()) {
                logger.info("Create walks is disabled in configuration");
                if (hasDBFlag) {
                    final String message = "DB rebuild is required, mismatch on config (false) and db (true) for " + source.getName();
                    logger.error(message);
                    throw new RuntimeException(message);
                }
                return;
            }

            if (hasDBFlag) {
                logger.info("Node present, assuming walks for closed already built in DB");
                return;
            }

            createWalksForClosed(source);
            addDBFlag(source);
            reportStats();
        });

        logger.info("started");
    }

    public Ready getReady() {
        return new Ready();
    }

    private void createWalksForClosed(GTFSSourceConfig source) {
        logger.info("Add walks for closed stations for " + source.getName());
        if (source.getStationClosures().isEmpty()) {
            logger.warn("No station closures are given for " + source.getName());
            return;
        }

        List<StationClosure> closures = source.getStationClosures();
        MarginInMeters range = MarginInMeters.of(config.getNearestStopForWalkingRangeKM());

        closures.forEach(closure -> {
            IdSet<Station> closedStationIds = closure.getStations();
            closedStationIds.stream().filter(filter::shouldInclude).
                forEach(closedStationId -> {
                    Station closedStation = stationRepository.getStationById(closedStationId);
                    if (closedStation.getGridPosition().isValid()) {
                        Set<Station> nearbyStations = stationLocations.nearestStationsUnsorted(closedStation, range).
                                filter(filter::shouldInclude).collect(Collectors.toSet());
                        createWalksInDB(closedStation, nearbyStations);
                    }
                });
        });
    }

    private void createWalksInDB(Station station, Set<Station> neighbours) {
        try(TimedTransaction timedTransaction = new TimedTransaction(database, logger, "create walks for " +station.getId())) {
            Transaction txn = timedTransaction.transaction();
            addWalkRelationships(txn, filter, station, neighbours);
            timedTransaction.commit();
        }
    }

    private boolean hasDBFlag(GTFSSourceConfig sourceConfig) {
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
                node = txn.createNode(GraphLabel.WALK_FOR_CLOSED_ENABLED);
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

    private void addWalkRelationships(Transaction txn, GraphFilter filter, Station closedStation, Set<Station> others) {
        Node closedNode = graphQuery.getStationOrGrouped(txn, closedStation);
        if (closedNode==null) {
            String msg = "Could not find database node for from: " + closedStation.getId();
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        double mph = config.getWalkingMPH();
        logger.debug("Adding walk relations from " + closedStation.getId() + " to " + asIds(others));
        others.stream().filter(filter::shouldInclude).forEach(otherStation -> {

            int cost = CoordinateTransforms.calcCostInMinutes(closedStation, otherStation, mph);

            Node otherNode = graphQuery.getStationOrGrouped(txn, otherStation);
            if (otherNode==null) {
                String msg = "Could not find database node for to: " + otherStation.getId();
                logger.error(msg);
                throw new RuntimeException(msg);
            }

            Relationship fromClosed = closedNode.createRelationshipTo(otherNode, WALKS_FROM);
            Relationship fromOther = otherNode.createRelationshipTo(closedNode, WALKS_TO);

            GraphProps.setCostProp(fromClosed, cost);
            GraphProps.setCostProp(fromOther, cost);

            GraphProps.setProperty(fromClosed, otherStation);
            GraphProps.setProperty(fromOther, closedStation);
        });
    }

    public class Ready {
        private Ready() {

        }
    }


}

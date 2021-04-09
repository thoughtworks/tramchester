package com.tramchester.graph;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.Station;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.StationLocationsRepository;
import com.tramchester.graph.graphbuild.CreateNodesAndRelationships;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.graphbuild.GraphFilter;
import com.tramchester.repository.StationRepository;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Set;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class CreateNeighbours extends CreateNodesAndRelationships {
    private static final Logger logger = LoggerFactory.getLogger(CreateNeighbours.class);

    private final GraphDatabase database;
    private final StationRepository repository;
    private final StationLocationsRepository stationLocations;
    private final GraphQuery graphQuery;
    private final TramchesterConfig config;
    private final double rangeInKM;
    private final GraphFilter filter;

    ///
    // finds stations within a specified distance and adds a direct relationship between them
    // this aids with routing where stops are very close together but are not linked in any other way
    ///

    // TODO Generalise to TransportRelationshipTypes.NEIGHBOUR with TransportMode property

    @Inject
    public CreateNeighbours(GraphDatabase database, GraphFilter filter, GraphQuery graphQuery, StationRepository repository,
                            StationLocationsRepository stationLocations, TramchesterConfig config, NodeTypeRepository nodeTypeRepository) {
        super(database, nodeTypeRepository);
        this.database = database;
        this.filter = filter;
        this.graphQuery = graphQuery;
        this.repository = repository;
        this.stationLocations = stationLocations;
        this.config = config;
        this.rangeInKM = config.getDistanceToNeighboursKM();
    }

    @PostConstruct
    public void start() {
        if (!config.getCreateNeighbours()) {
            logger.warn("Create neighbours is disabled in configuration");
            return;
        }

        logger.info("Start");
        try(Transaction txn = database.beginTx()) {
            buildWithNoCommit(txn);
            logger.info("Commit");
            txn.commit();
        }
        catch (Exception exception) {
            logger.error("Caught exception during neighbouring station add", exception);
        }
        reportStats();
        logger.info("Started");
    }

    private void buildWithNoCommit(Transaction txn) {
        if (hasDBFlag(txn)) {
            logger.warn("Node NEIGHBOURS_ENABLED present, assuming neighbours already built");
        } else {
            Set<Station> stations = repository.getStations();
            logger.info(format("Adding neighbouring stations for %s stations and range %s KM", stations.size(), rangeInKM));
            stations.forEach(station -> {
                if (filter.shouldInclude(station)) {
                    Stream<Station> nearby = stationLocations.nearestStationsUnsorted(station, rangeInKM);
                    addNeighbourRelationships(txn, filter, station, nearby);
                }
            });
            addDBFlag(txn);
        }
    }

    private boolean hasDBFlag(Transaction txn) {
        return graphQuery.hasAnyNodesWithLabel(txn, GraphBuilder.Labels.NEIGHBOURS_ENABLED);
    }

    private void addDBFlag(Transaction txn) {
        txn.createNode(GraphBuilder.Labels.NEIGHBOURS_ENABLED);
    }

    private void addNeighbourRelationships(Transaction txn, GraphFilter filter, Station from, Stream<Station> others) {
        Node fromNode = graphQuery.getStationNode(txn, from);
        if (fromNode==null) {
            String msg = "Could not find database node for from: " + from.getId();
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        double mph = config.getWalkingMPH();
        logger.info("Adding neighbour relations from " + from.getId()); // + " to " + HasId.asIds(others));
        others.filter(filter::shouldInclude).forEach(to -> {

            Node toNode = graphQuery.getStationNode(txn, to);
            if (toNode==null) {
                String msg = "Could not find database node for to: " + to.getId();
                logger.error(msg);
                throw new RuntimeException(msg);
            }

            int cost = CoordinateTransforms.calcCostInMinutes(from, to, mph);
            if (!addNeighbourRelationship(fromNode, toNode, cost)) {
                logger.info(format("Already neighbour link from %s to %s", from.getId(), to.getId()));
            }
        });
    }

}

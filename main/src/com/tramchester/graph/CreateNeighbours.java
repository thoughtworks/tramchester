package com.tramchester.graph;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.HasId;
import com.tramchester.domain.places.Station;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.StationLocationsRepository;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.graphbuild.GraphFilter;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.repository.StationRepository;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.*;
import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Singleton
public class CreateNeighbours implements Startable {
    private static final Logger logger = LoggerFactory.getLogger(CreateNeighbours.class);

    private final GraphDatabase database;
    private final StationRepository repository;
    private final StationLocationsRepository stationLocations;
    private final GraphQuery graphQuery;
    private final TramchesterConfig config;
    private final double rangeInKM;
    private final GraphFilter filter;

    @Inject
    public CreateNeighbours(GraphDatabase database, GraphFilter filter, GraphQuery graphQuery, StationRepository repository,
                            StationLocationsRepository stationLocations, TramchesterConfig config) {
        this.database = database;
        this.filter = filter;
        this.graphQuery = graphQuery;
        this.repository = repository;
        this.stationLocations = stationLocations;
        this.config = config;
        this.rangeInKM = config.getDistanceToNeighboursKM();
    }

    @PostConstruct
    @Override
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
        logger.info("Started");
    }

    public void buildWithNoCommit(Transaction txn) {
        if (hasDBFlag(txn)) {
            logger.warn("Node NEIGHBOURS_ENABLED present, assuming neighbours already built");
        } else {
            Set<Station> stations = repository.getStations();
            logger.info(format("Adding neighbouring stations for %s stations and range %s KM", stations.size(), rangeInKM));
            stations.forEach(station -> {
                if (filter.shouldInclude(station)) {
                    Set<Station> nearby = stationLocations.nearestStationsUnsorted(station, rangeInKM);
                    addRelationships(txn, filter, station, nearby);
                }
            });
            addDBFlag(txn);
        }
    }

    private boolean hasDBFlag(Transaction txn) {
        ResourceIterator<Node> query = database.findNodes(txn, GraphBuilder.Labels.NEIGHBOURS_ENABLED);
        List<Node> nodes = query.stream().collect(Collectors.toList());
        return !nodes.isEmpty();
    }

    private void addDBFlag(Transaction txn) {
        txn.createNode(GraphBuilder.Labels.NEIGHBOURS_ENABLED);
    }

    private void addRelationships(Transaction txn, GraphFilter filter, Station station, Set<Station> others) {
        logger.info("Adding neighbour relations from " + station.getId() + " to " + HasId.asIds(others));
        double mph = config.getWalkingMPH();
        final Node stationNode = graphQuery.getStationNode(txn, station);
        if (stationNode!=null) {
            others.stream().filter(filter::shouldInclude).forEach(other -> {
                Node otherNode = graphQuery.getStationNode(txn, other);
                RelationshipType relationType = getRelationType(other);
                Set<Long> already = new HashSet<>();
                stationNode.getRelationships(Direction.OUTGOING, relationType).
                        forEach(relationship -> already.add(relationship.getEndNode().getId()));

                if (!already.contains(otherNode.getId())) {
                    Relationship relationship = stationNode.createRelationshipTo(otherNode, relationType);
                    GraphProps.setCostProp(relationship, CoordinateTransforms.calcCostInMinutes(station, other, mph));
                } else {
                    logger.info("Relationship of type " + relationType + " already present from " + station + " to " + other);
                }
            });
        } else {
            logger.warn("Cannot add neighbours for station, no node found, station: "+station.getId());
        }
    }

    @NotNull
    private TransportRelationshipTypes getRelationType(Station station) {
        switch (station.getTransportMode()) {
            case Tram:
                return TransportRelationshipTypes.TRAM_NEIGHBOUR;
            case Bus:
                return TransportRelationshipTypes.BUS_NEIGHBOUR;
            case Train:
                return TransportRelationshipTypes.TRAIN_NEIGHBOUR;
            default:
                // TODO Train
                throw new RuntimeException("Unsupported mode " + station.getTransportMode());
        }
    }


    @Override
    public void stop() {
        // no-op
    }
}

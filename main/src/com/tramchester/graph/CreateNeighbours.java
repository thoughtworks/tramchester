package com.tramchester.graph;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.HasId;
import com.tramchester.domain.places.Station;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.StationLocationsRepository;
import com.tramchester.graph.graphbuild.GraphFilter;
import com.tramchester.repository.StationRepository;
import org.apache.commons.collections.bag.HashBag;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.*;
import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

import static java.lang.String.format;

public class CreateNeighbours implements Startable {
    private static final Logger logger = LoggerFactory.getLogger(CreateNeighbours.class);

    private final GraphDatabase database;
    private final StationRepository repository;
    private final StationLocationsRepository stationLocations;
    private final GraphQuery graphQuery;
    private final TramchesterConfig config;
    private final double rangeInKM;
    private final GraphFilter filter;

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

    @Override
    public void start() {
        logger.info("Start");
        try(Transaction txn = database.beginTx()) {
            buildWithNoCommit(txn);
            logger.info("Commit");
            txn.commit();
        }
        catch (Exception exception) {
            logger.error("Caught expception during nieghbouring station add", exception);
        }
        logger.info("Started");
    }

    public void buildWithNoCommit(Transaction txn) {
        Set<Station> stations = repository.getStations();
        logger.info(format("Adding neighbouring stations for %s stations and range %s KM", stations.size(), rangeInKM));
        stations.forEach(station -> {
            if (filter.shouldInclude(station)) {
                Set<Station> nearby = stationLocations.nearestStationsUnsorted(station, rangeInKM);
                addRelationships(txn, filter, station, nearby);
            }
        });
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
                    relationship.setProperty(GraphStaticKeys.COST, CoordinateTransforms.calcCostInMinutes(station, other, mph));
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

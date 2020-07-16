package com.tramchester.graph;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.HasId;
import com.tramchester.domain.places.Station;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.StationLocationsRepository;
import com.tramchester.repository.StationRepository;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public CreateNeighbours(GraphDatabase database, GraphQuery graphQuery, StationRepository repository,
                            StationLocationsRepository stationLocations, TramchesterConfig config) {
        this.database = database;
        this.graphQuery = graphQuery;
        this.repository = repository;
        this.stationLocations = stationLocations;
        this.config = config;
        this.rangeInKM = config.getDistanceToNeighboursKM();
    }

    public void buildWithNoCommit(Transaction txn) {
        Set<Station> stations = repository.getStations();
        logger.info(format("Adding neighbouring stations for %s stations and range %s KM", stations.size(), rangeInKM));
        stations.forEach(station -> {
            Set<Station> nearby = stationLocations.nearestStationsUnsorted(station, rangeInKM);
            addRelationships(txn, station, nearby);
        });
    }

    private void addRelationships(Transaction txn, Station station, Set<Station> others) {
        logger.info("Adding neighbour relations from " + station.getId() + " to " + HasId.asIds(others));
        double mph = config.getWalkingMPH();
        final Node stationNode = graphQuery.getStationNode(txn, station);
        if (stationNode!=null) {
            others.forEach(other -> {
                Node otherNode = graphQuery.getStationNode(txn, other);
                RelationshipType relationType = other.isTram() ? TransportRelationshipTypes.TRAM_NEIGHBOUR : TransportRelationshipTypes.BUS_NEIGHBOUR;
                Relationship relationship = stationNode.createRelationshipTo(otherNode, relationType);
                relationship.setProperty(GraphStaticKeys.COST, CoordinateTransforms.calcCostInMinutes(station, other, mph));
            });
        } else {
            logger.warn("Cannot add neighbours for station, no node found, station: "+station.getId());
        }
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

    @Override
    public void stop() {
        // no-op
    }
}

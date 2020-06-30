package com.tramchester.graph;

import com.tramchester.domain.places.Station;
import com.tramchester.geo.StationLocationsRepository;
import com.tramchester.repository.StationRepository;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

import static java.lang.String.format;

public class CreateNeighbours {
    private static final Logger logger = LoggerFactory.getLogger(CreateNeighbours.class);

    private final GraphDatabase database;
    private final StationRepository repository;
    private final StationLocationsRepository stationLocations;
    private final GraphQuery graphQuery;
    private final double rangeInKM;

    public CreateNeighbours(GraphDatabase database, GraphQuery graphQuery, StationRepository repository,
                            StationLocationsRepository stationLocations, double rangeInKM) {
        this.database = database;
        this.graphQuery = graphQuery;
        this.repository = repository;
        this.stationLocations = stationLocations;
        this.rangeInKM = rangeInKM;
    }

    public void buildWithNoCommit(Transaction txn) {
        Set<Station> stations = repository.getStations();
        logger.info(format("Adding neighbouring stations for %s stations and range %s KM", stations.size(), rangeInKM));
        stations.forEach(station -> {
            List<Station> nearby = stationLocations.nearestStationsUnsorted(station, rangeInKM);
            addRelationships(txn, station,nearby);
        });
    }

    private void addRelationships(Transaction txn, Station station, List<Station> others) {
        Node stationNode = graphQuery.getStationNode(txn, station);
        others.forEach(otherStation -> {
            Node otherNode = graphQuery.getStationNode(txn, otherStation);
            stationNode.createRelationshipTo(otherNode, TransportRelationshipTypes.NEIGHBOUR);
            //otherNode.createRelationshipTo(stationNode, TransportRelationshipTypes.NEIGHBOUR);
        });
    }

}

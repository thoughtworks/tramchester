package com.tramchester.resources;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationWalk;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.StationLocations;
import com.tramchester.graph.*;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.graph.search.RouteCalculatorArriveBy;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.geo.CoordinateTransforms.calcCostInMinutes;
import static com.tramchester.graph.GraphStaticKeys.COST;
import static java.lang.String.format;

public class LocationJourneyPlanner {
    private static final Logger logger = LoggerFactory.getLogger(LocationJourneyPlanner.class);

    private final StationLocations stationLocations;
    private final TramchesterConfig config;
    private final RouteCalculator routeCalculator;
    private final RouteCalculatorArriveBy routeCalculatorArriveBy;
    private final NodeContentsRepository nodeOperations;
    private final GraphQuery graphQuery;
    private final GraphDatabase graphDatabase;
    private final NodeTypeRepository nodeTypeRepository;

    public LocationJourneyPlanner(StationLocations stationLocations, TramchesterConfig config, RouteCalculator routeCalculator,
                                  RouteCalculatorArriveBy routeCalculatorArriveBy, NodeContentsRepository nodeOperations,
                                  GraphQuery graphQuery, GraphDatabase graphDatabase, NodeTypeRepository nodeTypeRepository) {
        this.config = config;
        this.routeCalculator = routeCalculator;
        this.routeCalculatorArriveBy = routeCalculatorArriveBy;
        this.nodeOperations = nodeOperations;
        this.graphQuery = graphQuery;
        this.graphDatabase = graphDatabase;
        this.nodeTypeRepository = nodeTypeRepository;
        this.stationLocations = stationLocations;
    }

    public Stream<Journey> quickestRouteForLocation(Transaction txn, LatLong latLong, Station destination, JourneyRequest journeyRequest) {
        logger.info(format("Finding shortest path for %s --> %s on %s", latLong, destination, journeyRequest));

        List<Relationship> addedRelationships = new LinkedList<>();

        List<StationWalk> walksToStart = getStationWalks(latLong);

        Node startOfWalkNode = createWalkingNode(txn, latLong);

        walksToStart.forEach(stationWalk -> addedRelationships.add(createWalkRelationship(txn, startOfWalkNode, stationWalk,
                TransportRelationshipTypes.WALKS_TO)));

        Stream<Journey> journeys;
        if (journeyRequest.getArriveBy()) {
            journeys = routeCalculatorArriveBy.calculateRouteWalkAtStart(txn, startOfWalkNode, destination, journeyRequest);
        } else {
            journeys = routeCalculator.calculateRouteWalkAtStart(txn, startOfWalkNode, destination, journeyRequest);
        }

        //noinspection ResultOfMethodCallIgnored
        journeys.onClose(() -> removeWalkNodeAndRelationships(addedRelationships, startOfWalkNode));

        return journeys;
    }

    public Stream<Journey> quickestRouteForLocation(Transaction txn, Station start, LatLong destination, JourneyRequest journeyRequest) {
        logger.info(format("Finding shortest path for %s --> %s on %s", start, destination, journeyRequest));

        Set<Station> destinationStations = new HashSet<>();
        List<Relationship> addedRelationships = new LinkedList<>();

        List<StationWalk> walksToDest = getStationWalks(destination);
        Node midWalkNode = createMidWalkingNode(txn, destination);

        walksToDest.forEach(stationWalk -> {
            destinationStations.add(stationWalk.getStation());
            addedRelationships.add(createWalkRelationship(txn, midWalkNode, stationWalk, TransportRelationshipTypes.WALKS_FROM));
        });
        Node endWalk = createWalkingNode(txn, destination);
        Relationship relationshipTo = midWalkNode.createRelationshipTo(endWalk, TransportRelationshipTypes.FINISH_WALK);
        relationshipTo.setProperty(COST,0);
        addedRelationships.add(relationshipTo);

        Stream<Journey> journeys;
        if (journeyRequest.getArriveBy()) {
            journeys = routeCalculatorArriveBy.calculateRouteWalkAtEnd(txn, start, endWalk, destinationStations, journeyRequest);
        } else {
            journeys = routeCalculator.calculateRouteWalkAtEnd(txn, start, endWalk, destinationStations, journeyRequest);
        }

        //noinspection ResultOfMethodCallIgnored
        journeys.onClose(() -> removeWalkNodeAndRelationships(addedRelationships, midWalkNode, endWalk));

        return journeys;
    }


    public Stream<Journey> quickestRouteForLocation(Transaction txn, LatLong startLatLong, LatLong destLatLong, JourneyRequest journeyRequest) {
        logger.info(format("Finding shortest path for %s --> %s on %s", startLatLong, destLatLong, journeyRequest));

        List<Relationship> addedRelationships = new LinkedList<>();

        // Add Walk at the Start
        List<StationWalk> walksAtStart = getStationWalks(startLatLong);
        Node startNode = createWalkingNode(txn, startLatLong);
        walksAtStart.forEach(stationWalk -> addedRelationships.add(createWalkRelationship(txn, startNode, stationWalk,
                TransportRelationshipTypes.WALKS_TO)));

        // Add Walks at the end
        Set<Station> destinationStations = new HashSet<>();
        List<StationWalk> walksToDest = getStationWalks(destLatLong);
        Node midWalkNode = createWalkingNode(txn, destLatLong);
        walksToDest.forEach(stationWalk -> {
            destinationStations.add(stationWalk.getStation());
            addedRelationships.add(createWalkRelationship(txn, midWalkNode, stationWalk, TransportRelationshipTypes.WALKS_FROM));
        });
        Node endWalk = createWalkingNode(txn, destLatLong);
        Relationship relationshipTo = midWalkNode.createRelationshipTo(endWalk, TransportRelationshipTypes.FINISH_WALK);
        relationshipTo.setProperty(COST,0);
        addedRelationships.add(relationshipTo);

        /// CALC
        Stream<Journey> journeys;
        if (journeyRequest.getArriveBy()) {
            journeys = routeCalculatorArriveBy.calculateRouteWalkAtStartAndEnd(txn, startNode,  endWalk, destinationStations,
                    journeyRequest);
        } else {
            journeys = routeCalculator.calculateRouteWalkAtStartAndEnd(txn, startNode, endWalk, destinationStations, journeyRequest);
        }

        //noinspection ResultOfMethodCallIgnored
        journeys.onClose(() -> removeWalkNodeAndRelationships(addedRelationships, startNode, midWalkNode, endWalk));

        return journeys;

    }

    private Relationship createWalkRelationship(Transaction txn, Node walkNode, StationWalk stationWalk, TransportRelationshipTypes direction) {
        Station walkStation = stationWalk.getStation();
        int cost = stationWalk.getCost();
        logger.info(format("Add walking %s relationship between %s to %s cost %s direction",
                direction, walkStation, walkNode,  cost));

        Relationship walkingRelationship;
        Node stationNode = graphQuery.getStationNode(txn, walkStation);
        if (direction==TransportRelationshipTypes.WALKS_FROM) {
            walkingRelationship = stationNode.createRelationshipTo(walkNode, direction);
        } else {
            walkingRelationship = walkNode.createRelationshipTo(stationNode, direction);
        }

        walkingRelationship.setProperty(COST, cost);
        walkingRelationship.setProperty(GraphStaticKeys.STATION_ID, walkStation.getId().getGraphId());
        return walkingRelationship;
    }

    private Node createWalkingNode(Transaction txn, LatLong origin) {
        Node startOfWalkNode = nodeTypeRepository.createQueryNode(graphDatabase, txn);
        startOfWalkNode.setProperty(GraphStaticKeys.Walk.LAT, origin.getLat());
        startOfWalkNode.setProperty(GraphStaticKeys.Walk.LONG, origin.getLon());
        startOfWalkNode.setProperty(GraphStaticKeys.ID, origin.toString());
        logger.info(format("Added walking node at %s as node %s", origin, startOfWalkNode));
        return startOfWalkNode;
    }

    private Node createMidWalkingNode(Transaction txn, LatLong origin) {
        Node startOfWalkNode = nodeTypeRepository.createQueryNodeMidPoint(graphDatabase, txn);
        startOfWalkNode.setProperty(GraphStaticKeys.Walk.LAT, origin.getLat());
        startOfWalkNode.setProperty(GraphStaticKeys.Walk.LONG, origin.getLon());
        startOfWalkNode.setProperty(GraphStaticKeys.ID, origin.toString());
        logger.info(format("Adding mid walking node at %s as node %s", origin, startOfWalkNode));
        return startOfWalkNode;
    }

    // TODO Creation and deletion of walk nodes into own facade which can then be auto-closable
    @Deprecated
    private void removeWalkNodeAndRelationships(List<Relationship> relationshipsToDelete, Node... nodesToDelete) {
        logger.info("Removed added walks and walk node(s)");
        relationshipsToDelete.forEach(relationship -> {
            nodeOperations.deleteFromCostCache(relationship);
            relationship.delete();
        });
        for (Node node : nodesToDelete) {
            nodeTypeRepository.deleteQueryNode(node);
        }
    }

    private List<StationWalk> getStationWalks(LatLong latLong) { //, double rangeInKM) {
        int num = config.getNumOfNearestStopsForWalking();
        double rangeInKM = config.getNearestStopRangeKM();
        List<Station> nearbyStations = stationLocations.getNearestStationsTo(latLong, num, rangeInKM);
        List<StationWalk> stationWalks = createWalks(latLong, nearbyStations);
        logger.info(format("Stops within %s of %s are [%s]", rangeInKM, latLong, stationWalks));
        return stationWalks;
    }

    private List<StationWalk> createWalks(LatLong latLong, List<Station> startStations) {
        return startStations.stream().map(station ->
                new StationWalk(station, calcCostInMinutes(latLong, station, config.getWalkingMPH()))).collect(Collectors.toList());
    }

}

package com.tramchester.resources;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationWalk;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.graph.*;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.graph.search.RouteCalculatorArriveBy;
import com.tramchester.services.SpatialService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.graph.GraphStaticKeys.COST;
import static java.lang.String.format;

public class LocationJourneyPlanner {
    private static final Logger logger = LoggerFactory.getLogger(LocationJourneyPlanner.class);

    private static final double EARTH_RADIUS = 3958.75;

    private final SpatialService spatialService;
    private final TramchesterConfig config;
    private final RouteCalculator routeCalculator;
    private final RouteCalculatorArriveBy routeCalculatorArriveBy;
    private final CachedNodeOperations nodeOperations;
    private final GraphQuery graphQuery;
    private final GraphDatabase graphDatabase;

    public LocationJourneyPlanner(SpatialService spatialService, TramchesterConfig config, RouteCalculator routeCalculator,
                                  RouteCalculatorArriveBy routeCalculatorArriveBy, CachedNodeOperations nodeOperations,
                                  GraphQuery graphQuery, GraphDatabase graphDatabase) {
        this.spatialService = spatialService;
        this.config = config;
        this.routeCalculator = routeCalculator;
        this.routeCalculatorArriveBy = routeCalculatorArriveBy;
        this.nodeOperations = nodeOperations;
        this.graphQuery = graphQuery;
        this.graphDatabase = graphDatabase;
    }

    public Stream<Journey> quickestRouteForLocation(Transaction txn, LatLong latLong, Station destination, JourneyRequest journeyRequest) {
        logger.info(format("Finding shortest path for %s --> %s on %s", latLong, destination, journeyRequest));

        List<Relationship> addedRelationships = new LinkedList<>();

        List<StationWalk> walksToStart = getStationWalks(latLong,  config.getNearestStopRangeKM());

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

        List<Station> destinationStations = new ArrayList<>();
        List<Relationship> addedRelationships = new LinkedList<>();

        List<StationWalk> walksToDest = getStationWalks(destination,  config.getNearestStopRangeKM());
        Node midWalkNode = createWalkingNode(txn, destination);

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
        List<StationWalk> walksAtStart = getStationWalks(startLatLong,  config.getNearestStopRangeKM());
        Node startNode = createWalkingNode(txn, startLatLong);
        walksAtStart.forEach(stationWalk -> addedRelationships.add(createWalkRelationship(txn, startNode, stationWalk,
                TransportRelationshipTypes.WALKS_TO)));

        // Add Walks at the end
        List<Station> destinationStations = new ArrayList<>();
        List<StationWalk> walksToDest = getStationWalks(destLatLong,  config.getNearestStopRangeKM());
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
        walkingRelationship.setProperty(GraphStaticKeys.STATION_ID, walkStation.getId());
        return walkingRelationship;
    }

    private Node createWalkingNode(Transaction txn, LatLong origin) {
        Node startOfWalkNode = nodeOperations.createQueryNode(graphDatabase, txn);
        startOfWalkNode.setProperty(GraphStaticKeys.Walk.LAT, origin.getLat());
        startOfWalkNode.setProperty(GraphStaticKeys.Walk.LONG, origin.getLon());
        logger.info(format("Added walking node at %s as node %s", origin, startOfWalkNode));
        return startOfWalkNode;
    }

    private void removeWalkNodeAndRelationships(List<Relationship> relationshipsToDelete, Node... nodesToDelete) {
        logger.info("Removed added walks and walk node(s)");
        relationshipsToDelete.forEach(relationship -> {
            nodeOperations.deleteFromCache(relationship);
            relationship.delete();
        });
        for (Node node : nodesToDelete) {
            nodeOperations.deleteNode(node);
        }
    }

    private List<StationWalk> getStationWalks(LatLong latLong, double rangeInKM) {
        int num = config.getNumOfNearestStopsForWalking();
        List<Station> nearbyStationIds = spatialService.getNearestStationsTo(latLong, num, rangeInKM);
        List<StationWalk> stationWalks = nearestStations(latLong, nearbyStationIds);
        logger.info(format("Stops within %s of %s are [%s]", rangeInKM, latLong, stationWalks));
        return stationWalks;
    }

    private List<StationWalk> nearestStations(LatLong latLong, List<Station> startStations) {
        return startStations.stream().map(station ->
                new StationWalk(station, findCostInMinutes(latLong, station))).collect(Collectors.toList());
    }

    // TODO Use Grid Position instead of LatLong??
    private int findCostInMinutes(LatLong latLong, Location station) {

        double distanceInMiles = distFrom(latLong, station.getLatLong());
        double hours = distanceInMiles / config.getWalkingMPH();
        return (int)Math.ceil(hours * 60D);
    }

    private double distFrom(LatLong point1, LatLong point2) {
        double lat1 = point1.getLat();
        double lat2 = point2.getLat();
        double diffLat = Math.toRadians(lat2-lat1);
        double diffLong = Math.toRadians(point2.getLon()-point1.getLon());
        double sineDiffLat = Math.sin(diffLat / 2D);
        double sineDiffLong = Math.sin(diffLong / 2D);

        double a = Math.pow(sineDiffLat, 2) + Math.pow(sineDiffLong, 2)
                * Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2));

        double fractionOfRadius = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return EARTH_RADIUS * fractionOfRadius;
    }

}

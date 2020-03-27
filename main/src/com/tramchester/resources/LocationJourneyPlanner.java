package com.tramchester.resources;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.*;
import com.tramchester.repository.StationRepository;
import com.tramchester.services.SpatialService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.graph.GraphStaticKeys.COST;
import static java.lang.String.format;

public class LocationJourneyPlanner {
    private static final Logger logger = LoggerFactory.getLogger(LocationJourneyPlanner.class);

    private static final double EARTH_RADIUS = 3958.75;

    private final SpatialService spatialService;
    private TramchesterConfig config;
    private final RouteCalculator routeCalculator;
    private final RouteCalculatorArriveBy routeCalculatorArriveBy;
    private final StationRepository stationRepository;
    private final CachedNodeOperations nodeOperations;
    private final NodeIdQuery stationIndexs;
    private final GraphDatabase graphDatabase;

    public LocationJourneyPlanner(SpatialService spatialService, TramchesterConfig config, RouteCalculator routeCalculator,
                                  RouteCalculatorArriveBy routeCalculatorArriveBy, StationRepository stationRepository,
                                  CachedNodeOperations nodeOperations, NodeIdQuery nodeIdQuery, GraphDatabase graphDatabase) {
        this.spatialService = spatialService;
        this.config = config;
        this.routeCalculator = routeCalculator;
        this.routeCalculatorArriveBy = routeCalculatorArriveBy;
        this.stationRepository = stationRepository;
        this.nodeOperations = nodeOperations;
        this.stationIndexs = nodeIdQuery;
        this.graphDatabase = graphDatabase;
    }

    public Stream<Journey> quickestRouteForLocation(LatLong latLong, Station destination, TramTime queryTime,
                                                    TramServiceDate queryDate, boolean arriveBy) {
        logger.info(format("Finding shortest path for %s --> %s on %s at %s", latLong, destination, queryDate, queryTime));
        List<StationWalk> walksToStart = getStationWalks(latLong,  config.getNearestStopRangeKM());

        Node startOfWalkNode = createWalkingNode(latLong);
        List<Relationship> addedRelationships = new LinkedList<>();

        walksToStart.forEach(stationWalk -> addedRelationships.add(createWalkRelationship(startOfWalkNode, stationWalk,
                TransportRelationshipTypes.WALKS_TO)));

        Stream<Journey> journeys;
        if (arriveBy) {
            journeys = routeCalculatorArriveBy.calculateRouteWalkAtStart(startOfWalkNode, destination,
                    queryTime, queryDate);
        } else {
            journeys = routeCalculator.calculateRouteWalkAtStart(startOfWalkNode, destination,
                    queryTime, queryDate);
        }

        //noinspection ResultOfMethodCallIgnored
        journeys.onClose(() -> removeWalkNodeAndRelationships(addedRelationships, startOfWalkNode));

        return journeys;
    }

    public Stream<Journey> quickestRouteForLocation(String startId, LatLong destination, TramTime queryTime,
                                                    TramServiceDate queryDate, boolean arriveBy) {
        logger.info(format("Finding shortest path for %s --> %s on %s at %s", startId, destination, queryDate, queryTime));
        List<StationWalk> walksToDest = getStationWalks(destination,  config.getNearestStopRangeKM());

        List<Relationship> addedRelationships = new LinkedList<>();
        List<Station> destinationStations = new ArrayList<>();
        Node midWalkNode = createWalkingNode(destination);

        walksToDest.forEach(stationWalk -> {
            destinationStations.add(stationWalk.getStation());
            addedRelationships.add(createWalkRelationship(midWalkNode, stationWalk, TransportRelationshipTypes.WALKS_FROM));
        });
        Node endWalk = createWalkingNode(destination);
        Relationship relationshipTo = midWalkNode.createRelationshipTo(endWalk, TransportRelationshipTypes.FINISH_WALK);
        relationshipTo.setProperty(COST,0);
        addedRelationships.add(relationshipTo);

        Stream<Journey> journeys;
        if (arriveBy) {
            journeys = routeCalculatorArriveBy.calculateRouteWalkAtEnd(startId, endWalk, destinationStations,
                    queryTime, queryDate);
        } else {
            journeys = routeCalculator.calculateRouteWalkAtEnd(startId, endWalk, destinationStations,
                    queryTime, queryDate);
        }

        //noinspection ResultOfMethodCallIgnored
        journeys.onClose(() -> removeWalkNodeAndRelationships(addedRelationships, midWalkNode, endWalk));

        return journeys;
    }

    private Relationship createWalkRelationship(Node walkNode, StationWalk stationWalk, TransportRelationshipTypes direction) {
        Station walkStation = stationWalk.getStation();
        int cost = stationWalk.getCost();
        logger.info(format("Add walking %s relationship between %s to %s cost %s direction",
                direction, walkStation, walkNode,  cost));

        Relationship walkingRelationship;
        Node stationNode = stationIndexs.getStationNode(walkStation.getId());
        if (direction==TransportRelationshipTypes.WALKS_FROM) {
            walkingRelationship = stationNode.createRelationshipTo(walkNode, direction);
        } else {
            walkingRelationship = walkNode.createRelationshipTo(stationNode, direction);
        }

        walkingRelationship.setProperty(COST, cost);
        walkingRelationship.setProperty(GraphStaticKeys.STATION_ID, walkStation.getId());
        return walkingRelationship;
    }

    private Node createWalkingNode(LatLong origin) {
        Node startOfWalkNode = nodeOperations.createQueryNode(graphDatabase);
        startOfWalkNode.setProperty(GraphStaticKeys.Station.LAT, origin.getLat());
        startOfWalkNode.setProperty(GraphStaticKeys.Station.LONG, origin.getLon());
        logger.info(format("Added walking node at %s as node %s", origin, startOfWalkNode));
        return startOfWalkNode;
    }

    private void removeWalkNodeAndRelationships(List<Relationship> relationshipsToDelete, Node... nodesToDelete) {
        logger.info("Removed added walks and start of walk node");
        relationshipsToDelete.forEach(relationship -> {
            nodeOperations.deleteFromCache(relationship);
            relationship.delete();
        });
        for (int i = 0; i <nodesToDelete.length; i++) {
            nodeOperations.deleteNode(nodesToDelete[i]);
        }
    }

    private List<StationWalk> getStationWalks(LatLong latLong, double rangeInKM) {
        int num = config.getNumOfNearestStopsForWalking();
        List<String> nearbyStationIds = spatialService.getNearestStationsTo(latLong, num, rangeInKM);
        List<StationWalk> stationWalks = nearestStations(latLong, nearbyStationIds);
        logger.info(format("Stops within %s of %s are [%s]", rangeInKM, latLong, stationWalks));
        return stationWalks;
    }

    private List<StationWalk> nearestStations(LatLong latLong, List<String> startIds) {
        List<Station> stations = startIds.stream().
                filter(stationRepository::hasStationId).
                map(stationRepository::getStation).
                collect(Collectors.toList());

        return stations.stream().map(station ->
                new StationWalk(station, findCostInMinutes(latLong, station))).collect(Collectors.toList());
    }

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

package com.tramchester.resources;

import com.javadocmd.simplelatlng.LatLng;
import com.javadocmd.simplelatlng.LatLngTool;
import com.javadocmd.simplelatlng.util.LengthUnit;
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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

public class LocationJourneyPlanner {
    private static final Logger logger = LoggerFactory.getLogger(LocationJourneyPlanner.class);

    private final String queryNodeName = "BEGIN";

    private final double walkingSpeed;
    private final SpatialService spatialService;
    private final RouteCalculator routeCalculator;
    private final RouteCalculatorArriveBy routeCalculatorArriveBy;
    private final StationRepository stationRepository;
    private final CachedNodeOperations nodeOperations;
    private final StationIndexs stationIndexs;

    public LocationJourneyPlanner(SpatialService spatialService, TramchesterConfig config,
                                  RouteCalculator routeCalculator, RouteCalculatorArriveBy routeCalculatorArriveBy, StationRepository stationRepository,
                                  CachedNodeOperations nodeOperations, StationIndexs stationIndexs) {
        this.spatialService = spatialService;
        this.walkingSpeed = config.getWalkingMPH();
        this.routeCalculator = routeCalculator;
        this.routeCalculatorArriveBy = routeCalculatorArriveBy;
        this.stationRepository = stationRepository;
        this.nodeOperations = nodeOperations;
        this.stationIndexs = stationIndexs;
    }

    public Stream<Journey> quickestRouteForLocation(LatLong latLong, String destinationId, TramTime queryTime,
                                                    TramServiceDate queryDate, boolean arriveBy) {
        logger.info(format("Finding shortest path for %s --> %s on %s at %s", latLong, destinationId, queryDate, queryTime));
        List<StationWalk> walksToStart = getStationWalks(latLong);

        Node startOfWalkNode = createWalkingNode(latLong);
        List<Relationship> addedRelationships = new LinkedList<>();

        walksToStart.forEach(stationWalk -> {
            addedRelationships.add(createWalkRelationship(startOfWalkNode, stationWalk, TransportRelationshipTypes.WALKS_TO));
        });

        Stream<Journey> journeys;
        if (arriveBy) {
            journeys = routeCalculatorArriveBy.calculateRouteWalkAtStart(startOfWalkNode, destinationId,
                    queryTime, queryDate);
        } else {
            journeys = routeCalculator.calculateRouteWalkAtStart(startOfWalkNode, destinationId,
                    queryTime, queryDate);
        }

        journeys.onClose(() -> {
            removeWalkNodeAndRelationships(addedRelationships, startOfWalkNode);
        });

        return journeys;
    }

    public Stream<Journey> quickestRouteForLocation(String startId, LatLong destination, TramTime queryTime,
                                                    TramServiceDate queryDate, boolean arriveBy) {
        logger.info(format("Finding shortest path for %s --> %s on %s at %s", startId, destination, queryDate, queryTime));
        List<StationWalk> walksToDest = getStationWalks(destination);

        List<Relationship> addedRelationships = new LinkedList<>();
        List<String> destinationStationIds = new ArrayList<>();
        Node endOfWalk = createWalkingNode(destination);

        walksToDest.forEach(stationWalk -> {
            String walkStationId = stationWalk.getStationId();
            destinationStationIds.add(walkStationId);
            addedRelationships.add(createWalkRelationship(endOfWalk, stationWalk, TransportRelationshipTypes.WALKS_FROM));
        });

        Stream<Journey> journeys;
        if (arriveBy) {
            journeys = routeCalculatorArriveBy.calculateRouteWalkAtEnd(startId, endOfWalk, destinationStationIds,
                    queryTime, queryDate);
        } else {
            journeys = routeCalculator.calculateRouteWalkAtEnd(startId, endOfWalk, destinationStationIds,
                    queryTime, queryDate);
        }

        journeys.onClose(() -> {
            removeWalkNodeAndRelationships(addedRelationships, endOfWalk);
        });

        return journeys;
    }

    private List<StationWalk> getStationWalks(LatLong latLong) {
        List<String> nearbyStations = spatialService.getNearestStationsTo(latLong, Integer.MAX_VALUE);
        return nearestStations(latLong, nearbyStations);
    }

    private Relationship createWalkRelationship(Node walkNode, StationWalk stationWalk, TransportRelationshipTypes direction) {
        String walkStationId = stationWalk.getStationId();
        int cost = stationWalk.getCost();
        logger.info(format("Add walking relationship from %s to %s cost %s direction %s",
                walkStationId, walkNode,  cost, direction));

        Relationship walkingRelationship;
        Node stationNode = stationIndexs.getStationNode(walkStationId);
        if (direction==TransportRelationshipTypes.WALKS_FROM) {
            walkingRelationship = stationNode.createRelationshipTo(walkNode, direction);
        } else {
            walkingRelationship = walkNode.createRelationshipTo(stationNode, direction);
        }

        walkingRelationship.setProperty(GraphStaticKeys.COST, cost);
        walkingRelationship.setProperty(GraphStaticKeys.STATION_ID, walkStationId);
        return walkingRelationship;
    }

    private Node createWalkingNode(LatLong origin) {
        Node startOfWalkNode = nodeOperations.createQueryNode(stationIndexs);
        startOfWalkNode.setProperty(GraphStaticKeys.Station.LAT, origin.getLat());
        startOfWalkNode.setProperty(GraphStaticKeys.Station.LONG, origin.getLon());
        startOfWalkNode.setProperty(GraphStaticKeys.Station.NAME, queryNodeName);
        logger.info(format("Added walking node at %s as node %s", origin, startOfWalkNode));
        return startOfWalkNode;
    }

    private void removeWalkNodeAndRelationships(List<Relationship> addedRelationships, Node endOfWalk) {
        logger.info("Removed added walks and start of walk node");
        addedRelationships.forEach(Relationship::delete);
        nodeOperations.deleteNode(endOfWalk);
    }

    private List<StationWalk> nearestStations(LatLong latLong, List<String> startIds) {
        List<Location> starts = startIds.stream().map(stationRepository::getStation).
                filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());

        return starts.stream().map(station ->
                new StationWalk(station, findCostInMinutes(latLong, station))).collect(Collectors.toList());
    }

    private int findCostInMinutes(LatLong latLong, Location station) {
        LatLng point1 = LatLong.getLatLng(latLong);
        LatLng point2 = LatLong.getLatLng(station.getLatLong());

        double distanceInMiles = LatLngTool.distance(point1, point2, LengthUnit.MILE);
        double hours = distanceInMiles / walkingSpeed;
        return (int)Math.ceil(hours * 60D);
    }
}

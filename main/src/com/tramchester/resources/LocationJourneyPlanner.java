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
    private final StationRepository stationRepository;
    private final CachedNodeOperations nodeOperations;
    private final StationIndexs stationIndexs;

    public LocationJourneyPlanner(SpatialService spatialService, TramchesterConfig config,
                                  RouteCalculator routeCalculator, StationRepository stationRepository,
                                  CachedNodeOperations nodeOperations, StationIndexs stationIndexs) {
        this.spatialService = spatialService;
        this.walkingSpeed = config.getWalkingMPH();
        this.routeCalculator = routeCalculator;
        this.stationRepository = stationRepository;
        this.nodeOperations = nodeOperations;
        this.stationIndexs = stationIndexs;
    }

    public Stream<Journey> quickestRouteForLocation(LatLong latLong, String destinationId, TramTime queryTime,
                                                    TramServiceDate queryDate) {

        logger.info(format("Finding shortest path for %s --> %s on %s at %s",
                latLong, destinationId, queryDate, queryTime));

        List<String> nearbyStations = spatialService.getNearestStationsTo(latLong, Integer.MAX_VALUE);

        logger.info(format("Found %s stations close to %s", nearbyStations.size(), latLong));
        return createJourneyPlan(latLong, nearbyStations, destinationId, queryTime, queryDate);
    }

    public Stream<Journey> quickestRouteForLocation(String startId, LatLong latLong, TramTime queryTime,
                                                    TramServiceDate queryDate) {
        List<String> nearbyStations = spatialService.getNearestStationsTo(latLong, Integer.MAX_VALUE);

        List<StationWalk> walksToDest = nearestStations(latLong, nearbyStations);
        return routeCalculator.calculateRouteWalkAtEnd(startId, latLong, walksToDest, queryTime, queryDate);
    }

    private Stream<Journey> createJourneyPlan(LatLong latLong, List<String> startIds, String destinationId, TramTime queryTime,
                                              TramServiceDate queryDate) {
        List<StationWalk> walksToStart = nearestStations(latLong, startIds);

        Node startOfWalkNode = createWalkingNode(latLong);

        // todo extract method
        List<Relationship> addedWalks = new LinkedList<>();

        walksToStart.forEach(stationWalk -> {
            String walkStationId = stationWalk.getStationId();
            Node stationNode = stationIndexs.getStationNode(walkStationId);
            int cost = stationWalk.getCost();
            logger.info(format("Add walking relationship from %s to %s cost %s", startOfWalkNode, walkStationId, cost));
            Relationship walkingRelationship = startOfWalkNode.createRelationshipTo(stationNode, TransportRelationshipTypes.WALKS_TO);
            walkingRelationship.setProperty(GraphStaticKeys.COST, cost);
            walkingRelationship.setProperty(GraphStaticKeys.STATION_ID, walkStationId);
            addedWalks.add(walkingRelationship);
        });

        Stream<Journey> journeys = routeCalculator.calculateRouteWalkAtStart(startOfWalkNode, walksToStart, destinationId,
                queryTime, queryDate);

        journeys.onClose(() -> {
            logger.info("Removed added walks and start of walk node");
            addedWalks.forEach(Relationship::delete);
            nodeOperations.deleteNode(startOfWalkNode);
        });

        return journeys;
    }

    private Node createWalkingNode(LatLong origin) {
        Node startOfWalkNode = nodeOperations.createQueryNode(stationIndexs);
        startOfWalkNode.setProperty(GraphStaticKeys.Station.LAT, origin.getLat());
        startOfWalkNode.setProperty(GraphStaticKeys.Station.LONG, origin.getLon());
        startOfWalkNode.setProperty(GraphStaticKeys.Station.NAME, queryNodeName);
        logger.info(format("Added walking node at %s as node %s", origin, startOfWalkNode));
        return startOfWalkNode;
    }

    private List<StationWalk> nearestStations(LatLong latLong, List<String> startIds) {
        List<Location> starts = startIds.stream().map(id -> stationRepository.getStation(id)).
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

package com.tramchester.resources;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationWalk;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.StationLocations;
import com.tramchester.graph.*;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.caches.NodeTypeRepository;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.graph.search.RouteCalculatorArriveBy;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.geo.CoordinateTransforms.calcCostInMinutes;
import static java.lang.String.format;

@LazySingleton
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

    @Inject
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

    public Stream<Journey> quickestRouteForLocation(Transaction txn, LatLong latLong, Station destination,
                                                    JourneyRequest journeyRequest) {
        logger.info(format("Finding shortest path for %s --> %s (%s) for %s", latLong,
                destination.getId(), destination.getName(), journeyRequest));

        List<Relationship> addedRelationships = new LinkedList<>();

        List<StationWalk> walksToStart = getStationWalks(latLong);

        Node startOfWalkNode = createWalkingNode(txn, latLong, journeyRequest);

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
        logger.info(format("Finding shortest path for %s (%s) --> %s for %s", start.getId(), start.getName(), destination, journeyRequest));

        Set<Station> destinationStations = new HashSet<>();
        List<Relationship> addedRelationships = new LinkedList<>();

        List<StationWalk> walksToDest = getStationWalks(destination);
        Node endWalk = createWalkingNode(txn, destination, journeyRequest);

        walksToDest.forEach(stationWalk -> {
            destinationStations.add(stationWalk.getStation());
            addedRelationships.add(createWalkRelationship(txn, endWalk, stationWalk, TransportRelationshipTypes.WALKS_FROM));
        });

        Stream<Journey> journeys;
        if (journeyRequest.getArriveBy()) {
            journeys = routeCalculatorArriveBy.calculateRouteWalkAtEnd(txn, start, endWalk, destinationStations, journeyRequest);
        } else {
            journeys = routeCalculator.calculateRouteWalkAtEnd(txn, start, endWalk, destinationStations, journeyRequest);
        }

        //noinspection ResultOfMethodCallIgnored
        journeys.onClose(() -> removeWalkNodeAndRelationships(addedRelationships, endWalk));

        return journeys;
    }

    public Stream<Journey> quickestRouteForLocation(Transaction txn, LatLong startLatLong, LatLong destLatLong, JourneyRequest journeyRequest) {
        logger.info(format("Finding shortest path for %s --> %s on %s", startLatLong, destLatLong, journeyRequest));

        List<Relationship> addedRelationships = new LinkedList<>();

        // Add Walk at the Start
        List<StationWalk> walksAtStart = getStationWalks(startLatLong);
        Node startNode = createWalkingNode(txn, startLatLong, journeyRequest);
        walksAtStart.forEach(stationWalk -> addedRelationships.add(createWalkRelationship(txn, startNode, stationWalk,
                TransportRelationshipTypes.WALKS_TO)));

        // Add Walks at the end
        Set<Station> destinationStations = new HashSet<>();
        List<StationWalk> walksToDest = getStationWalks(destLatLong);
        // TODO is mid walk node still needed?
        Node midWalkNode = createWalkingNode(txn, destLatLong, journeyRequest);
        walksToDest.forEach(stationWalk -> {
            destinationStations.add(stationWalk.getStation());
            addedRelationships.add(createWalkRelationship(txn, midWalkNode, stationWalk, TransportRelationshipTypes.WALKS_FROM));
        });
        Node endWalk = createWalkingNode(txn, destLatLong, journeyRequest);
        Relationship relationshipTo = midWalkNode.createRelationshipTo(endWalk, TransportRelationshipTypes.FINISH_WALK);
        GraphProps.setCostProp(relationshipTo, 0);
        addedRelationships.add(relationshipTo);

        /// CALC
        Stream<Journey> journeys;
        if (journeyRequest.getArriveBy()) {
            journeys = routeCalculatorArriveBy.calculateRouteWalkAtStartAndEnd(txn, startNode,  endWalk, destinationStations,
                    journeyRequest);
        } else {
            journeys = routeCalculator.calculateRouteWalkAtStartAndEnd(txn, startNode, endWalk, destinationStations,
                    journeyRequest);
        }

        //noinspection ResultOfMethodCallIgnored
        journeys.onClose(() -> removeWalkNodeAndRelationships(addedRelationships, startNode, midWalkNode, endWalk));

        return journeys;

    }

    private Relationship createWalkRelationship(Transaction txn, Node walkNode, StationWalk stationWalk, TransportRelationshipTypes direction) {
        Station walkStation = stationWalk.getStation();
        int cost = stationWalk.getCost();
        logger.info(format("Add %s relationship between %s (%s) to %s cost %s direction",
                direction, walkStation.getId(), walkStation.getName(), walkNode,  cost));

        Relationship walkingRelationship;
        Node stationNode = graphQuery.getStationOrGrouped(txn, walkStation);

        if (direction==TransportRelationshipTypes.WALKS_FROM) {
            walkingRelationship = stationNode.createRelationshipTo(walkNode, direction);
        } else {
            walkingRelationship = walkNode.createRelationshipTo(stationNode, direction);
        }

        GraphProps.setCostProp(walkingRelationship, cost);
        GraphProps.setProperty(walkingRelationship, walkStation);
        return walkingRelationship;
    }

    private Node createWalkingNode(Transaction txn, LatLong origin, JourneyRequest journeyRequest) {
        Node startOfWalkNode = nodeTypeRepository.createQueryNode(graphDatabase, txn);
        GraphProps.setLatLong(startOfWalkNode, origin);
        GraphProps.setWalkId(startOfWalkNode, origin, journeyRequest.getUid());
        logger.info(format("Added walking node at %s as %s", origin, startOfWalkNode));
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

    private List<StationWalk> getStationWalks(LatLong latLong) {
        int maxResults = config.getNumOfNearestStopsForWalking();
        double rangeInKM = config.getNearestStopForWalkingRangeKM();
        List<Station> nearbyStations = stationLocations.getNearestStationsTo(latLong, maxResults, rangeInKM);
        List<StationWalk> stationWalks = createWalks(latLong, nearbyStations);
        logger.info(format("Stops within %s of %s are [%s]", rangeInKM, latLong, stationWalks));
        return stationWalks;
    }

    private List<StationWalk> createWalks(LatLong latLong, List<Station> startStations) {
        return startStations.stream().map(station ->
                new StationWalk(station, calcCostInMinutes(latLong, station, config.getWalkingMPH()))).collect(Collectors.toList());
    }

}

package com.tramchester.resources;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.NumberOfChanges;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationWalk;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.*;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphQuery;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.filters.GraphFilter;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.search.BetweenRoutesCostRepository;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.graph.search.RouteCalculatorArriveBy;
import com.tramchester.graph.search.RouteToRouteCosts;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.geo.CoordinateTransforms.calcCostInMinutes;
import static com.tramchester.graph.TransportRelationshipTypes.WALKS_FROM;
import static com.tramchester.graph.TransportRelationshipTypes.WALKS_TO;
import static java.lang.String.format;

@LazySingleton
public class LocationJourneyPlanner {
    private static final Logger logger = LoggerFactory.getLogger(LocationJourneyPlanner.class);

    private final StationLocationsRepository stationLocations;
    private final GraphFilter graphFilter;
    private final TramchesterConfig config;
    private final RouteCalculator routeCalculator;
    private final RouteCalculatorArriveBy routeCalculatorArriveBy;
    private final NodeContentsRepository nodeOperations;
    private final GraphQuery graphQuery;
    private final GraphDatabase graphDatabase;
    private final MarginInMeters margin;
    private final BetweenRoutesCostRepository routeToRouteCosts;

    @Inject
    public LocationJourneyPlanner(StationLocations stationLocations, TramchesterConfig config, RouteCalculator routeCalculator,
                                  RouteCalculatorArriveBy routeCalculatorArriveBy, NodeContentsRepository nodeOperations,
                                  GraphQuery graphQuery, GraphDatabase graphDatabase,
                                  GraphFilter graphFilter, RouteToRouteCosts routeToRouteCosts) {
        logger.info("created");
        this.config = config;
        this.routeCalculator = routeCalculator;
        this.routeCalculatorArriveBy = routeCalculatorArriveBy;
        this.nodeOperations = nodeOperations;
        this.graphQuery = graphQuery;
        this.graphDatabase = graphDatabase;
        this.stationLocations = stationLocations;
        this.graphFilter = graphFilter;
        this.margin = MarginInMeters.of(config.getNearestStopForWalkingRangeKM());
        this.routeToRouteCosts = routeToRouteCosts;
    }

    public Stream<Journey> quickestRouteForLocation(Transaction txn, LatLong start, Location<?> destination,
                                                    JourneyRequest journeyRequest) {
        logger.info(format("Finding shortest path for %s --> %s (%s) for %s", start,
                destination.getId(), destination.getName(), journeyRequest));

        GridPosition startGrid = CoordinateTransforms.getGridPosition(start);
        if (!stationLocations.getBounds().within(margin, startGrid)) {
            logger.warn(format("Start %s not within %s of station bounds %s", startGrid, margin, stationLocations.getBounds()));
        }

        Node startOfWalkNode = createWalkingNode(txn, start, journeyRequest.getUid());
        Set<StationWalk> walksToStart = getStationWalks(start);

        List<Relationship> addedRelationships = createWalksToStations(txn, startOfWalkNode, walksToStart);

        if (addedRelationships.isEmpty()) {
            removeWalkNodeAndRelationships(addedRelationships, startOfWalkNode);
            logger.warn("No relationships can be added from walking node to stations for start " +start);
            return Stream.empty();
        }

        Stream<Journey> journeys;
        NumberOfChanges numberOfChanges = findNumberChanges(walksToStart, destination);
        if (journeyRequest.getArriveBy()) {
            journeys = routeCalculatorArriveBy.calculateRouteWalkAtStart(txn, walksToStart, startOfWalkNode, destination, journeyRequest, numberOfChanges);
        } else {
            journeys = routeCalculator.calculateRouteWalkAtStart(txn, walksToStart, startOfWalkNode, destination, journeyRequest, numberOfChanges);
        }

        //noinspection ResultOfMethodCallIgnored
        journeys.onClose(() -> removeWalkNodeAndRelationships(addedRelationships, startOfWalkNode));
        return journeys;
    }

    @NotNull
    public List<Relationship> createWalksToStations(Transaction txn, Node startOfWalkNode, Set<StationWalk> walksToStart) {
        List<Relationship> addedRelationships = new LinkedList<>();
        walksToStart.forEach(stationWalk -> addedRelationships.add(createWalkRelationship(txn, startOfWalkNode, stationWalk,
                WALKS_TO)));
        return addedRelationships;
    }

    public Stream<Journey> quickestRouteForLocation(Transaction txn, Location<?> start, LatLong destination, JourneyRequest journeyRequest) {
        logger.info(format("Finding shortest path for %s (%s) --> %s for %s", start.getId(), start.getName(),
                destination, journeyRequest));

        GridPosition endGrid = CoordinateTransforms.getGridPosition(destination);
        if (!stationLocations.getBounds().within(margin, endGrid)) {
            logger.warn(format("Destination %s not within %s of station bounds %s", endGrid, margin, stationLocations.getBounds()));
        }

        if (!stationLocations.getBounds().contained(destination)) {
            logger.warn("Destination not within station bounds " + destination);
        }

        Set<StationWalk> walksToDest = getStationWalks(destination);
        if (walksToDest.isEmpty()) {
            logger.warn("Cannot find any walks from " + destination + " to stations");
            return Stream.empty();
        }

        Node endWalk = createWalkingNode(txn, destination, journeyRequest.getUid());
        List<Relationship> addedRelationships = new LinkedList<>();
        walksToDest.forEach(stationWalk -> addedRelationships.add(createWalkRelationship(txn, endWalk, stationWalk, WALKS_FROM)));

        LocationSet destinationStations = new LocationSet();
        walksToDest.forEach(stationWalk -> destinationStations.add(stationWalk.getStation()));

        NumberOfChanges numberOfChanges = findNumberChanges(start, walksToDest);

        Stream<Journey> journeys;
        if (journeyRequest.getArriveBy()) {
            journeys = routeCalculatorArriveBy.calculateRouteWalkAtEnd(txn, start, endWalk, destinationStations, journeyRequest, numberOfChanges);
        } else {
            journeys = routeCalculator.calculateRouteWalkAtEnd(txn, start, endWalk, destinationStations, journeyRequest, numberOfChanges);
        }

        //noinspection ResultOfMethodCallIgnored
        journeys.onClose(() -> removeWalkNodeAndRelationships(addedRelationships, endWalk));

        return journeys;
    }

    public Stream<Journey> quickestRouteForLocation(Transaction txn, LatLong startLatLong, LatLong destLatLong, JourneyRequest journeyRequest) {
        logger.info(format("Finding shortest path for %s --> %s on %s", startLatLong, destLatLong, journeyRequest));

        List<Relationship> addedRelationships = new LinkedList<>();

        // Add Walk at the Start
        Set<StationWalk> walksAtStart = getStationWalks(startLatLong);
        Node startNode = createWalkingNode(txn, startLatLong, journeyRequest.getUid());
        walksAtStart.forEach(stationWalk -> addedRelationships.add(createWalkRelationship(txn, startNode, stationWalk, WALKS_TO)));

        // Add Walks at the end
        LocationSet destinationStations = new LocationSet();
        Set<StationWalk> walksToDest = getStationWalks(destLatLong);
        Node endWalk = createWalkingNode(txn, destLatLong, journeyRequest.getUid());

        walksToDest.forEach(stationWalk -> {
            destinationStations.add(stationWalk.getStation());
            addedRelationships.add(createWalkRelationship(txn, endWalk, stationWalk, WALKS_FROM));
        });

        NumberOfChanges numberOfChanges = findNumberChanges(walksAtStart, walksToDest);

        /// CALC
        Stream<Journey> journeys;
        if (journeyRequest.getArriveBy()) {
            journeys = routeCalculatorArriveBy.calculateRouteWalkAtStartAndEnd(txn, walksAtStart, startNode,  endWalk, destinationStations,
                    journeyRequest, numberOfChanges);
        } else {
            journeys = routeCalculator.calculateRouteWalkAtStartAndEnd(txn, walksAtStart, startNode, endWalk, destinationStations,
                    journeyRequest, numberOfChanges);
        }

        //noinspection ResultOfMethodCallIgnored
        journeys.onClose(() -> removeWalkNodeAndRelationships(addedRelationships, startNode, endWalk));
        return journeys;
    }

    public Relationship createWalkRelationship(Transaction txn, Node walkNode, StationWalk stationWalk,
                                                TransportRelationshipTypes direction) {
        Station walkStation = stationWalk.getStation();
        int cost = stationWalk.getCost();

        Relationship walkingRelationship;
        Node stationNode = graphQuery.getStationNode(txn, walkStation);
        if (stationNode==null) {
            throw new RuntimeException("Could not find node for " + walkStation);
        }

        if (direction == WALKS_FROM) {
            walkingRelationship = stationNode.createRelationshipTo(walkNode, direction);
            logger.info(format("Add %s relationship %s (%s) to node %s cost %s",
                    direction, walkStation.getId(), walkStation.getName(), walkNode.getId(),  cost));
        } else {
            walkingRelationship = walkNode.createRelationshipTo(stationNode, direction);
            logger.info(format("Add %s relationship between node %s to %s (%s) cost %s",
                    direction, walkNode.getId(), walkStation.getId(), walkStation.getName(), cost));
        }

        GraphProps.setCostProp(walkingRelationship, cost);
        GraphProps.setMaxCostProp(walkingRelationship, cost);
        GraphProps.setProperty(walkingRelationship, walkStation);
        return walkingRelationship;
    }

    public Node createWalkingNode(Transaction txn, LatLong origin, UUID uniqueId) {
        Node startOfWalkNode = graphDatabase.createNode(txn, GraphLabel.QUERY_NODE);
        GraphProps.setLatLong(startOfWalkNode, origin);
        GraphProps.setWalkId(startOfWalkNode, origin, uniqueId);
        logger.info(format("Added walking node at %s as %s", origin, startOfWalkNode));
        return startOfWalkNode;
    }

    // TODO Creation and deletion of walk nodes into own facade which can then be auto-closable
    @Deprecated
    public void removeWalkNodeAndRelationships(List<Relationship> relationshipsToDelete, Node... nodesToDelete) {
        logger.info("Removed added walks and walk node(s)");
        relationshipsToDelete.forEach(relationship -> {
            nodeOperations.deleteFromCostCache(relationship);
            relationship.delete();
        });
        for (Node node : nodesToDelete) {
            node.delete();
        }
    }

    public Set<StationWalk> getStationWalks(LatLong latLong) {

        int maxResults = config.getNumOfNearestStopsForWalking();
        List<Station> nearbyStationsWithComposites = stationLocations.nearestStationsSorted(latLong, maxResults, margin);

        if (nearbyStationsWithComposites.isEmpty()) {
            logger.warn(format("Failed to find stations within %s of %s", margin, latLong));
            return Collections.emptySet();
        }

        List<Station> filtered = nearbyStationsWithComposites.stream()
                //.filter(station -> !station.isStationGroup())
                .filter(graphFilter::shouldInclude).collect(Collectors.toList());

        Set<StationWalk> stationWalks = createWalks(latLong, filtered);
        logger.info(format("Stops within %s of %s are [%s]", maxResults, latLong, stationWalks));
        return stationWalks;
    }

    private NumberOfChanges findNumberChanges(Location<?> start, Set<StationWalk> walksToDest) {
        LocationSet destinations = walksToDest.stream().map(StationWalk::getStation).collect(LocationSet.stationCollector());
        return routeToRouteCosts.getNumberOfChanges(LocationSet.singleton(start), destinations);
    }

    private NumberOfChanges findNumberChanges(Set<StationWalk> walksToStart, Location<?> destination) {
        LocationSet starts = walksToStart.stream().map(StationWalk::getStation).collect(LocationSet.stationCollector());
        return routeToRouteCosts.getNumberOfChanges(starts, LocationSet.singleton(destination));
    }

    private NumberOfChanges findNumberChanges(Set<StationWalk> walksAtStart, Set<StationWalk> walksToDest) {
        LocationSet destinations = walksToDest.stream().map(StationWalk::getStation).collect(LocationSet.stationCollector());
        LocationSet starts = walksAtStart.stream().map(StationWalk::getStation).collect(LocationSet.stationCollector());
        return routeToRouteCosts.getNumberOfChanges(starts, destinations);
    }

    private Set<StationWalk> createWalks(LatLong latLong, List<Station> startStations) {
        return startStations.stream().map(station ->
                new StationWalk(station, calcCostInMinutes(latLong, station, config.getWalkingMPH()))).collect(Collectors.toSet());
    }

}

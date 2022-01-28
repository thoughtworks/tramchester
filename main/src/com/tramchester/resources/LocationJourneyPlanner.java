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
import static com.tramchester.graph.TransportRelationshipTypes.WALKS_FROM_STATION;
import static com.tramchester.graph.TransportRelationshipTypes.WALKS_TO_STATION;
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

    public Stream<Journey> quickestRouteForLocation(Transaction txn, Location<?> start, Location<?> destination,
                                                    JourneyRequest journeyRequest) {
        logger.info(format("Finding shortest path for %s --> %s (%s) for %s", start, destination.getId(), destination.getName(), journeyRequest));
        boolean walkAtStart = start.getLocationType().isWalk();
        boolean walkAtEnd = destination.getLocationType().isWalk();

        if (walkAtStart && walkAtEnd) {
            return quickestRouteForLocation(txn, start.getLatLong(), destination.getLatLong(), journeyRequest);
        }
        if (walkAtStart) {
            return quickestRouteForLocation(txn, start.getLatLong(), destination, journeyRequest);
        }
        if (walkAtEnd) {
            return quickestRouteForLocation(txn, start, destination.getLatLong(), journeyRequest);
        }

        // station => station
        if (journeyRequest.getArriveBy()) {
            return routeCalculatorArriveBy.calculateRoute(txn, start, destination, journeyRequest);
        } else {
            return routeCalculator.calculateRoute(txn, start, destination, journeyRequest);
        }
    }

    private Stream<Journey> quickestRouteForLocation(Transaction txn, LatLong start, Location<?> destination,
                                                    JourneyRequest journeyRequest) {
        logger.info(format("Finding shortest path for %s --> %s (%s) for %s", start,
                destination.getId(), destination.getName(), journeyRequest));

        GridPosition startGrid = CoordinateTransforms.getGridPosition(start);
        if (!stationLocations.getBounds().within(margin, startGrid)) {
            logger.warn(format("Start %s not within %s of station bounds %s", startGrid, margin, stationLocations.getBounds()));
        }

        Set<StationWalk> walksToStart = getStationWalks(start);
        if (walksToStart.isEmpty()) {
            logger.warn("No walks to start found " +start);
            return Stream.empty();
        }

        WalkNodesAndRelationships nodesAndRelationships = new WalkNodesAndRelationships(txn, graphDatabase, graphQuery, nodeOperations);
        Node startOfWalkNode = nodesAndRelationships.createWalkingNode(start, journeyRequest);
        nodesAndRelationships.createWalksToStart(startOfWalkNode, walksToStart);

        Stream<Journey> journeys;
        NumberOfChanges numberOfChanges = findNumberChanges(walksToStart, destination);
        if (journeyRequest.getArriveBy()) {
            journeys = routeCalculatorArriveBy.calculateRouteWalkAtStart(txn, walksToStart, startOfWalkNode, destination, journeyRequest, numberOfChanges);
        } else {
            journeys = routeCalculator.calculateRouteWalkAtStart(txn, walksToStart, startOfWalkNode, destination, journeyRequest, numberOfChanges);
        }

        //noinspection ResultOfMethodCallIgnored
        journeys.onClose(nodesAndRelationships::delete);
        return journeys;
    }

    private Stream<Journey> quickestRouteForLocation(Transaction txn, Location<?> start, LatLong destination, JourneyRequest journeyRequest) {
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

        WalkNodesAndRelationships nodesAndRelationships = new WalkNodesAndRelationships(txn, graphDatabase, graphQuery, nodeOperations);

        Node endWalk = nodesAndRelationships.createWalkingNode(destination, journeyRequest);
        List<Relationship> addedRelationships = new LinkedList<>();

        nodesAndRelationships.createWalksToDest(endWalk, walksToDest);

        nodesAndRelationships.addAll(addedRelationships);

        LocationSet destinationStations = walksToDest.stream().
                map(StationWalk::getStation).collect(LocationSet.stationCollector());

        NumberOfChanges numberOfChanges = findNumberChanges(start, walksToDest);

        Stream<Journey> journeys;
        if (journeyRequest.getArriveBy()) {
            journeys = routeCalculatorArriveBy.calculateRouteWalkAtEnd(txn, start, endWalk, destinationStations, journeyRequest, numberOfChanges);
        } else {
            journeys = routeCalculator.calculateRouteWalkAtEnd(txn, start, endWalk, destinationStations, journeyRequest, numberOfChanges);
        }

        //noinspection ResultOfMethodCallIgnored
        journeys.onClose(nodesAndRelationships::delete);

        return journeys;
    }

    private Stream<Journey> quickestRouteForLocation(Transaction txn, LatLong startLatLong, LatLong destLatLong, JourneyRequest journeyRequest) {
        logger.info(format("Finding shortest path for %s --> %s on %s", startLatLong, destLatLong, journeyRequest));

        WalkNodesAndRelationships nodesAndRelationships = new WalkNodesAndRelationships(txn, graphDatabase, graphQuery, nodeOperations);

        // Add Walk at the Start
        Set<StationWalk> walksAtStart = getStationWalks(startLatLong);
        Node startNode = nodesAndRelationships.createWalkingNode(startLatLong, journeyRequest);
        nodesAndRelationships.createWalksToStart(startNode, walksAtStart);

        // Add Walks at the end
        Set<StationWalk> walksToDest = getStationWalks(destLatLong);
        Node endWalk = nodesAndRelationships.createWalkingNode(destLatLong, journeyRequest);
        nodesAndRelationships.createWalksToDest(endWalk, walksToDest);

        // where destination walks take us
        LocationSet destinationStations = walksToDest.stream().
                map(StationWalk::getStation).collect(LocationSet.stationCollector());

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
        journeys.onClose(nodesAndRelationships::delete);
        return journeys;
    }

    public Set<StationWalk> getStationWalks(LatLong latLong) {

        int maxResults = config.getNumOfNearestStopsForWalking();
        List<Station> nearbyStationsWithComposites = stationLocations.nearestStationsSorted(latLong, maxResults, margin);

        if (nearbyStationsWithComposites.isEmpty()) {
            logger.warn(format("Failed to find stations within %s of %s", margin, latLong));
            return Collections.emptySet();
        }

        List<Station> filtered = nearbyStationsWithComposites.stream()
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
        return startStations.stream().
                map(station -> new StationWalk(station, calcCostInMinutes(latLong, station, config.getWalkingMPH()))).
                collect(Collectors.toSet());
    }

    private static class WalkNodesAndRelationships {

        private final GraphDatabase graphDatabase;
        private final GraphQuery graphQuery;
        private final NodeContentsRepository nodeOperations;
        private final Transaction txn;
        private final List<Relationship> relationships;
        private final List<Node> nodes;

        private WalkNodesAndRelationships(Transaction txn, GraphDatabase graphDatabase, GraphQuery graphQuery, NodeContentsRepository nodeOperations) {
            this.graphDatabase = graphDatabase;
            this.graphQuery = graphQuery;
            this.nodeOperations = nodeOperations;
            this.txn = txn;
            this.relationships = new ArrayList<>();
            this.nodes = new ArrayList<>();
        }

        public void delete() {
            logger.info("Removed added walks and walk node(s)");
            relationships.forEach(relationship -> {
                nodeOperations.deleteFromCostCache(relationship);
                relationship.delete();
            });
            for (Node node : nodes) {
                node.delete();
            }
        }

        public void add(Node node) {
            nodes.add(node);
        }

        public void addAll(List<Relationship> relationshipList) {
            relationships.addAll(relationshipList);
        }

        public Node createWalkingNode(LatLong start, JourneyRequest journeyRequest) {
            Node walkingNode = createWalkingNode(txn, start, journeyRequest.getUid());
            nodes.add(walkingNode);
            return walkingNode;
        }

        public void createWalksToStart(Node node, Set<StationWalk> walks) {
            createWalkRelationships(node, walks, WALKS_TO_STATION);
        }

        public void createWalksToDest(Node node, Set<StationWalk> walks) {
            createWalkRelationships(node, walks, WALKS_FROM_STATION);
        }

        private void createWalkRelationships(Node node, Set<StationWalk> walks, TransportRelationshipTypes direction) {
            List<Relationship> addedRelationships = new ArrayList<>();
            walks.forEach(stationWalk -> addedRelationships.add(createWalkRelationship(node, stationWalk, direction)));
            relationships.addAll(addedRelationships);
        }

        private Relationship createWalkRelationship(Node walkNode, StationWalk stationWalk, TransportRelationshipTypes direction) {
            Station walkStation = stationWalk.getStation();
            int cost = stationWalk.getCost();

            Relationship walkingRelationship;
            Node stationNode = graphQuery.getStationNode(txn, walkStation);
            if (stationNode==null) {
                throw new RuntimeException("Could not find node for " + walkStation);
            }

            if (direction == WALKS_FROM_STATION) {
                walkingRelationship = stationNode.createRelationshipTo(walkNode, direction);
                logger.info(format("Add %s relationship %s (%s) to node %s cost %s",
                        direction, walkStation.getId(), walkStation.getName(), walkNode.getId(),  cost));
            } else if (direction == WALKS_TO_STATION) {
                walkingRelationship = walkNode.createRelationshipTo(stationNode, direction);
                logger.info(format("Add %s relationship between node %s to %s (%s) cost %s",
                        direction, walkNode.getId(), walkStation.getId(), walkStation.getName(), cost));
            } else {
                throw new RuntimeException("Unknown direction " + direction);
            }

            GraphProps.setCostProp(walkingRelationship, cost);
            GraphProps.setMaxCostProp(walkingRelationship, cost);
            GraphProps.setProperty(walkingRelationship, walkStation);
            return walkingRelationship;
        }

        private Node createWalkingNode(Transaction txn, LatLong origin, UUID uniqueId) {
            Node startOfWalkNode = graphDatabase.createNode(txn, GraphLabel.QUERY_NODE);
            GraphProps.setLatLong(startOfWalkNode, origin);
            GraphProps.setWalkId(startOfWalkNode, origin, uniqueId);
            logger.info(format("Added walking node at %s as %s", origin, startOfWalkNode));
            return startOfWalkNode;
        }

    }

}

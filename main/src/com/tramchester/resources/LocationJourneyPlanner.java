package com.tramchester.resources;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.NumberOfChanges;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationWalk;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.geo.GridPosition;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.geo.StationLocations;
import com.tramchester.geo.StationLocationsRepository;
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
import com.tramchester.graph.search.routes.RouteToRouteCosts;
import com.tramchester.mappers.Geography;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final Geography geography;

    @Inject
    public LocationJourneyPlanner(StationLocations stationLocations, TramchesterConfig config, RouteCalculator routeCalculator,
                                  RouteCalculatorArriveBy routeCalculatorArriveBy, NodeContentsRepository nodeOperations,
                                  GraphQuery graphQuery, GraphDatabase graphDatabase,
                                  GraphFilter graphFilter, RouteToRouteCosts routeToRouteCosts, Geography geography) {
        this.geography = geography;
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
            return quickestRouteWalkAtStartAndEnd(txn, start, destination, journeyRequest);
        }
        if (walkAtStart) {
            return quickRouteWalkAtStart(txn, start, destination, journeyRequest);
        }
        if (walkAtEnd) {
            return quickestRouteWalkAtEnd(txn, start, destination, journeyRequest);
        }

        // station => station
        if (journeyRequest.getArriveBy()) {
            return routeCalculatorArriveBy.calculateRoute(txn, start, destination, journeyRequest);
        } else {
            return routeCalculator.calculateRoute(txn, start, destination, journeyRequest);
        }
    }

    private Stream<Journey> quickRouteWalkAtStart(Transaction txn, Location<?> start, Location<?> destination,
                                                  JourneyRequest journeyRequest) {
        logger.info(format("Finding shortest path for %s --> %s (%s) for %s", start,
                destination.getId(), destination.getName(), journeyRequest));

        GridPosition startGrid = start.getGridPosition();
        if (!stationLocations.getBounds().within(margin, startGrid)) {
            logger.warn(format("Start %s not within %s of station bounds %s", startGrid, margin, stationLocations.getBounds()));
        }

        Set<StationWalk> walksToStart = getStationWalks(start, journeyRequest.getRequestedModes());
        if (walksToStart.isEmpty()) {
            logger.warn("No walks to start found " +start);
            return Stream.empty();
        }

        WalkNodesAndRelationships nodesAndRelationships = new WalkNodesAndRelationships(txn, graphDatabase, graphQuery, nodeOperations);
        Node startOfWalkNode = nodesAndRelationships.createWalkingNode(start, journeyRequest);
        nodesAndRelationships.createWalksToStart(startOfWalkNode, walksToStart);

        Stream<Journey> journeys;
        NumberOfChanges numberOfChanges = findNumberChanges(walksToStart, destination, journeyRequest.getDate(), journeyRequest.getTimeRange());
        if (journeyRequest.getArriveBy()) {
            journeys = routeCalculatorArriveBy.calculateRouteWalkAtStart(txn, walksToStart, startOfWalkNode, destination, journeyRequest, numberOfChanges);
        } else {
            journeys = routeCalculator.calculateRouteWalkAtStart(txn, walksToStart, startOfWalkNode, destination, journeyRequest, numberOfChanges);
        }

        //noinspection ResultOfMethodCallIgnored
        journeys.onClose(nodesAndRelationships::delete);
        return journeys;
    }

    private Stream<Journey> quickestRouteWalkAtEnd(Transaction txn, Location<?> start, Location<?> destination,
                                                   JourneyRequest journeyRequest) {
        logger.info(format("Finding shortest path for %s (%s) --> %s for %s", start.getId(), start.getName(),
                destination, journeyRequest));

        GridPosition endGrid = destination.getGridPosition();
        if (!stationLocations.withinBounds(destination)) {
            logger.warn(format("Destination %s not within %s of station bounds %s", endGrid, margin, stationLocations.getBounds()));
        }

        if (!stationLocations.getBounds().contained(destination)) {
            logger.warn("Destination not within station bounds " + destination);
        }

        Set<StationWalk> walksToDest = getStationWalks(destination, journeyRequest.getRequestedModes());
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

        NumberOfChanges numberOfChanges = findNumberChanges(start, walksToDest, journeyRequest.getDate(), journeyRequest.getTimeRange());

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

    private Stream<Journey> quickestRouteWalkAtStartAndEnd(Transaction txn, Location<?> start, Location<?> dest,
                                                           JourneyRequest journeyRequest) {
        logger.info(format("Finding shortest path for %s --> %s on %s", start, dest, journeyRequest));

        WalkNodesAndRelationships nodesAndRelationships = new WalkNodesAndRelationships(txn, graphDatabase, graphQuery, nodeOperations);

        // Add Walk at the Start
        Set<StationWalk> walksAtStart = getStationWalks(start, journeyRequest.getRequestedModes());
        Node startNode = nodesAndRelationships.createWalkingNode(start, journeyRequest);
        nodesAndRelationships.createWalksToStart(startNode, walksAtStart);

        // Add Walks at the end
        Set<StationWalk> walksToDest = getStationWalks(dest, journeyRequest.getRequestedModes());
        Node endWalk = nodesAndRelationships.createWalkingNode(dest, journeyRequest);
        nodesAndRelationships.createWalksToDest(endWalk, walksToDest);

        // where destination walks take us
        LocationSet destinationStations = walksToDest.stream().
                map(StationWalk::getStation).collect(LocationSet.stationCollector());

        NumberOfChanges numberOfChanges = findNumberChanges(walksAtStart, walksToDest, journeyRequest.getDate(), journeyRequest.getTimeRange());

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

    public Set<StationWalk> getStationWalks(Location<?> location, Set<TransportMode> modes) {

        int maxResults = config.getNumOfNearestStopsForWalking();
        List<Station> nearbyStationsWithComposites = stationLocations.nearestStationsSorted(location, maxResults, margin, modes);

        if (nearbyStationsWithComposites.isEmpty()) {
            logger.warn(format("Failed to find stations within %s of %s", margin, location));
            return Collections.emptySet();
        }

        List<Station> filtered = nearbyStationsWithComposites.stream()
                .filter(graphFilter::shouldInclude).collect(Collectors.toList());

        Set<StationWalk> stationWalks = createWalks(location, filtered);
        logger.info(format("Stops within %s of %s are [%s]", maxResults, location, stationWalks));
        return stationWalks;
    }

    private NumberOfChanges findNumberChanges(Location<?> start, Set<StationWalk> walksToDest, TramDate date, TimeRange timeRange) {
        LocationSet destinations = walksToDest.stream().map(StationWalk::getStation).collect(LocationSet.stationCollector());
        return routeToRouteCosts.getNumberOfChanges(LocationSet.singleton(start), destinations, date, timeRange);
    }

    private NumberOfChanges findNumberChanges(Set<StationWalk> walksToStart, Location<?> destination, TramDate date, TimeRange timeRange) {
        LocationSet starts = walksToStart.stream().map(StationWalk::getStation).collect(LocationSet.stationCollector());
        return routeToRouteCosts.getNumberOfChanges(starts, LocationSet.singleton(destination), date, timeRange);
    }

    private NumberOfChanges findNumberChanges(Set<StationWalk> walksAtStart, Set<StationWalk> walksToDest, TramDate date, TimeRange timeRange) {
        LocationSet destinations = walksToDest.stream().map(StationWalk::getStation).collect(LocationSet.stationCollector());
        LocationSet starts = walksAtStart.stream().map(StationWalk::getStation).collect(LocationSet.stationCollector());
        return routeToRouteCosts.getNumberOfChanges(starts, destinations, date, timeRange);
    }

    private Set<StationWalk> createWalks(Location<?> location, List<Station> startStations) {
        return startStations.stream().
                map(station -> new StationWalk(station, calculateDuration(location, station))).
                collect(Collectors.toSet());
    }

    private Duration calculateDuration(Location<?> location, Station station) {
        return geography.getWalkingDuration(location, station);
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

        public Node createWalkingNode(Location<?> location, JourneyRequest journeyRequest) {
            Node walkingNode = createWalkingNode(txn, location.getLatLong(), journeyRequest.getUid());
            nodes.add(walkingNode);
            return walkingNode;
        }

        @Deprecated
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
            Duration cost = stationWalk.getCost();

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

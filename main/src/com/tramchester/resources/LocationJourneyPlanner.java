package com.tramchester.resources;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.places.CompositeStation;
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
import com.tramchester.domain.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.graph.search.RouteCalculatorArriveBy;
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

    @Inject
    public LocationJourneyPlanner(StationLocations stationLocations, TramchesterConfig config, RouteCalculator routeCalculator,
                                  RouteCalculatorArriveBy routeCalculatorArriveBy, NodeContentsRepository nodeOperations,
                                  GraphQuery graphQuery, GraphDatabase graphDatabase,
                                  GraphFilter graphFilter) {
        this.config = config;
        this.routeCalculator = routeCalculator;
        this.routeCalculatorArriveBy = routeCalculatorArriveBy;
        this.nodeOperations = nodeOperations;
        this.graphQuery = graphQuery;
        this.graphDatabase = graphDatabase;
        this.stationLocations = stationLocations;
        this.graphFilter = graphFilter;
        this.margin = MarginInMeters.of(config.getNearestStopForWalkingRangeKM());
    }

    public Stream<Journey> quickestRouteForLocation(Transaction txn, LatLong start, Station destination,
                                                    JourneyRequest journeyRequest) {
        logger.info(format("Finding shortest path for %s --> %s (%s) for %s", start,
                destination.getId(), destination.getName(), journeyRequest));

        GridPosition startGrid = CoordinateTransforms.getGridPosition(start);
        if (!stationLocations.getBounds().within(margin, startGrid)) {
            logger.warn(format("Start %s not within %s of station bounds %s", startGrid, margin, stationLocations.getBounds()));
        }

        Node startOfWalkNode = createWalkingNode(txn, start, journeyRequest);
        List<Relationship> addedRelationships = createWalksToStations(txn, start, startOfWalkNode);

        if (addedRelationships.isEmpty()) {
            removeWalkNodeAndRelationships(addedRelationships, startOfWalkNode);
            logger.warn("No relationships can be added from walking node to stations for start " +start);
            return Stream.empty();
        }

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

    @NotNull
    public List<Relationship> createWalksToStations(Transaction txn, LatLong latLong, Node startOfWalkNode) {
        List<StationWalk> walksToStart = getStationWalks(latLong);
        List<Relationship> addedRelationships = new LinkedList<>();
        walksToStart.forEach(stationWalk -> addedRelationships.add(createWalkRelationship(txn, startOfWalkNode, stationWalk,
                WALKS_TO)));
        return addedRelationships;
    }

    public Stream<Journey> quickestRouteForLocation(Transaction txn, Station start, LatLong destination, JourneyRequest journeyRequest) {
        logger.info(format("Finding shortest path for %s (%s) --> %s for %s", start.getId(), start.getName(),
                destination, journeyRequest));

        GridPosition endGrid = CoordinateTransforms.getGridPosition(destination);
        if (!stationLocations.getBounds().within(margin, endGrid)) {
            logger.warn(format("Destination %s not within %s of station bounds %s", endGrid, margin, stationLocations.getBounds()));
        }

        if (!stationLocations.getBounds().contained(destination)) {
            logger.warn("Destination not within station bounds " + destination);
        }

        List<StationWalk> walksToDest = getStationWalks(destination);
        if (walksToDest.isEmpty()) {
            logger.warn("Cannot find any walks from " + destination + " to stations");
            return Stream.empty();
        }

        Node endWalk = createWalkingNode(txn, destination, journeyRequest);
        List<Relationship> addedRelationships = new LinkedList<>();
        walksToDest.forEach(stationWalk -> addedRelationships.add(createWalkRelationship(txn, endWalk, stationWalk, WALKS_FROM)));

        Set<Station> destinationStations = new HashSet<>();
        walksToDest.forEach(stationWalk -> destinationStations.add(stationWalk.getStation()));

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
        walksAtStart.forEach(stationWalk -> addedRelationships.add(createWalkRelationship(txn, startNode, stationWalk, WALKS_TO)));

        // Add Walks at the end
        Set<Station> destinationStations = new HashSet<>();
        List<StationWalk> walksToDest = getStationWalks(destLatLong);
        Node endWalk = createWalkingNode(txn, destLatLong, journeyRequest);

        walksToDest.forEach(stationWalk -> {
            destinationStations.add(stationWalk.getStation());
            addedRelationships.add(createWalkRelationship(txn, endWalk, stationWalk, WALKS_FROM));
        });

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
        journeys.onClose(() -> removeWalkNodeAndRelationships(addedRelationships, startNode, endWalk));
        return journeys;
    }

    public Relationship createWalkRelationship(Transaction txn, Node walkNode, StationWalk stationWalk,
                                                TransportRelationshipTypes direction) {
        Station walkStation = stationWalk.getStation();
        int cost = stationWalk.getCost();
        logger.info(format("Add %s relationship between %s (%s) to %s cost %s direction",
                direction, walkStation.getId(), walkStation.getName(), walkNode,  cost));

        Relationship walkingRelationship;
        Node stationNode = graphQuery.getStationOrGrouped(txn, walkStation);

        if (direction== WALKS_FROM) {
            walkingRelationship = stationNode.createRelationshipTo(walkNode, direction);
        } else {
            walkingRelationship = walkNode.createRelationshipTo(stationNode, direction);
        }

        GraphProps.setCostProp(walkingRelationship, cost);
        GraphProps.setProperty(walkingRelationship, walkStation);
        return walkingRelationship;
    }

    public Node createWalkingNode(Transaction txn, LatLong origin, JourneyRequest journeyRequest) {
        Node startOfWalkNode = graphDatabase.createNode(txn, GraphLabel.QUERY_NODE);
        GraphProps.setLatLong(startOfWalkNode, origin);
        GraphProps.setWalkId(startOfWalkNode, origin, journeyRequest.getUid());
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

    public List<StationWalk> getStationWalks(LatLong latLong) {

        int maxResults = config.getNumOfNearestStopsForWalking();
        List<Station> nearbyStationsWithComposites = stationLocations.nearestStationsSorted(latLong, maxResults, margin);

        if (nearbyStationsWithComposites.isEmpty()) {
            logger.warn(format("Failed to find stations within %s of %s", margin, latLong));
            return Collections.emptyList();
        }

        List<Station> filtered = CompositeStation.expandStations(nearbyStationsWithComposites).stream()
                .filter(station -> !station.isComposite())
                .filter(graphFilter::shouldInclude).collect(Collectors.toList());

        List<StationWalk> stationWalks = createWalks(latLong, filtered);
        logger.info(format("Stops within %s of %s are [%s]", maxResults, latLong, stationWalks));
        return stationWalks;
    }

    private List<StationWalk> createWalks(LatLong latLong, List<Station> startStations) {
        return startStations.stream().map(station ->
                new StationWalk(station, calcCostInMinutes(latLong, station, config.getWalkingMPH()))).collect(Collectors.toList());
    }

}

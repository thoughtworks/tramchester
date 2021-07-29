package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationWalk;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphQuery;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.caches.LowestCostSeen;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.caches.PreviousVisits;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.search.*;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.ClosedStationsRepository;
import com.tramchester.repository.ServiceRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.resources.LocationJourneyPlanner;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.testSupport.reference.TramStations;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

public class MapPathToStagesViaStatesTest {
    private static final int TXN_TIMEOUT = 5*60;

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static IntegrationTramTestConfig config;

    private final LocalDate when = TestEnv.testDay();
    private Transaction txn;
    private RouteCalculator routeCalculator;
    private ProvidesLocalNow providesLocalNow;
    private ServiceRepository serviceRepository;
    private GraphQuery graphQuery;
    private StationRepository stationRepository;
    private NodeContentsRepository nodeContentsRepository;
    private PathToStages pathToStages;
    private LocationJourneyPlanner locationJourneyPlanner;
    private BetweenRoutesCostRepository routeToRouteCosts;
    private ClosedStationsRepository closedStationsRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
        database = componentContainer.get(GraphDatabase.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        txn = database.beginTx(TXN_TIMEOUT, TimeUnit.SECONDS);
        routeCalculator = componentContainer.get(RouteCalculator.class);
        providesLocalNow = componentContainer.get(ProvidesLocalNow.class);
        serviceRepository = componentContainer.get(ServiceRepository.class);
        nodeContentsRepository = componentContainer.get(NodeContentsRepository.class);
        graphQuery = componentContainer.get(GraphQuery.class);
        stationRepository = componentContainer.get(StationRepository.class);
        pathToStages = componentContainer.get(MapPathToStagesViaStates.class);
        locationJourneyPlanner = componentContainer.get(LocationJourneyPlanner.class);
        routeToRouteCosts = componentContainer.get(RouteToRouteCosts.class);
        closedStationsRepository = componentContainer.get(ClosedStationsRepository.class);
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldMapSimpleRoute() {
        TramTime queryTime = TramTime.of(9,15);
        int numChanges = 1;
        Station startStation = stationRepository.getStationById(Altrincham.getId());
        Station destination = stationRepository.getStationById(TraffordBar.getId());

        List<TransportStage<?, ?>> result = getStagesFor(queryTime, numChanges, startStation, destination);

        assertFalse(result.isEmpty());
        TransportStage<?, ?> firstStage = result.get(0);

        validateAltyToTraffordBar(firstStage, startStation, destination, TramTime.of(9, 19),
                17, NavigationRoad.getId(), 7);
    }

    @Test
    void shouldMapWithChange() {
        TramTime queryTime = TramTime.of(9,15);
        int numChanges = 1;
        Station startStation = stationRepository.getStationById(TramStations.ManAirport.getId());
        Station destination = stationRepository.getStationById(TramStations.Altrincham.getId());

        List<TransportStage<?, ?>> result = getStagesFor(queryTime, numChanges, startStation, destination);
        assertFalse(result.isEmpty());
        assertEquals(2, result.size());
        TransportStage<?, ?> firstStage = result.get(0);
        TransportStage<?, ?> secondStage = result.get(1);

        assertEquals(startStation, firstStage.getFirstStation());
        final IdFor<Station> changeStation = TraffordBar.getId();
        assertEquals(changeStation, firstStage.getLastStation().getId());
        assertEquals(changeStation, secondStage.getFirstStation().getId());
        assertEquals(destination, secondStage.getLastStation());
        assertNotEquals(firstStage.getRoute(), secondStage.getRoute());
        assertTrue(firstStage.hasBoardingPlatform());
        assertTrue(secondStage.hasBoardingPlatform());

        assertTrue(secondStage.getFirstDepartureTime().isAfter(firstStage.getExpectedArrivalTime()));
        assertEquals(42, firstStage.getDuration());
        assertEquals(18, secondStage.getDuration());

        assertEquals(17, firstStage.getPassedStopsCount());
        assertEquals(7, secondStage.getPassedStopsCount());
    }

    @Test
    void shouldMapWithWalkOnly() {
        TramTime queryTime = TramTime.of(9,15);
        int numChanges = 1;
        final LatLong start = TestEnv.nearAltrincham;
        Station destination = stationRepository.getStationById(TramStations.Altrincham.getId());

        List<TransportStage<?, ?>> result = getStagesFor(queryTime, numChanges, start, destination);
        assertEquals(1, result.size());

        TransportStage<?, ?> walkingStage = result.get(0);

        validateWalkTo(walkingStage, start, destination, 4);
    }

    @Test
    void shouldMapWithWalkAtStart() {
        TramTime queryTime = TramTime.of(9,15);
        int numChanges = 1;
        final LatLong start = TestEnv.nearAltrincham;
        Station destination = stationRepository.getStationById(TraffordBar.getId());
        Station endOfWalk = stationRepository.getStationById(Altrincham.getId());

        List<TransportStage<?, ?>> result = getStagesFor(queryTime, numChanges, start, destination);
        assertEquals(2, result.size());

        TransportStage<?, ?> walkingStage = result.get(0);
        validateWalkTo(walkingStage, start, endOfWalk, 4);

        TransportStage<?, ?> tramStage = result.get(1);
        final TramTime tramDepart = TramTime.of(9, 25);
        validateAltyToTraffordBar(tramStage, endOfWalk, destination, tramDepart,
                17, NavigationRoad.getId(), 7);
    }

    @Test
    void shouldMapWithWalkAtEnd() {
        TramTime queryTime = TramTime.of(9,15);
        int numChanges = 0;
        Station start = stationRepository.getStationById(Altrincham.getId());
        LatLong destinationLocation = TestEnv.nearStPetersSquare;

        List<TransportStage<?, ?>> result = getStagesFor(queryTime, numChanges, start, destinationLocation);
        assertEquals(2, result.size(), result.toString());
    }

    private void validateWalkTo(TransportStage<?, ?> walkingStage, LatLong start, Station endOfWalk, int walkDuration) {
        assertEquals(start, walkingStage.getFirstStation().getLatLong());
        assertEquals(endOfWalk, walkingStage.getLastStation());
        assertFalse(walkingStage.hasBoardingPlatform());
        assertEquals(walkDuration, walkingStage.getDuration());
        assertTrue(walkingStage.getCallingPoints().isEmpty());
        assertEquals(0, walkingStage.getPassedStopsCount());
    }

    private void validateAltyToTraffordBar(TransportStage<?, ?> stage, Station startStation, Station destination,
                                           TramTime firstDep, int stageDuration, IdFor<Station> firstCall, int passedStops) {
        assertEquals(startStation, stage.getFirstStation());
        assertEquals(destination, stage.getLastStation());

        final String routeName = stage.getRoute().getName();
        assertTrue(KnownTramRoute.AltrinchamPiccadilly.longName().equals(routeName) ||
                KnownTramRoute.AltrinchamManchesterBury.longName().equals(routeName));

        assertEquals(firstDep, stage.getFirstDepartureTime());
        assertEquals(stageDuration, stage.getDuration());
        assertEquals(firstDep.plusMinutes(stageDuration), stage.getExpectedArrivalTime());
        assertTrue(stage.hasBoardingPlatform());
        assertTrue(startStation.getPlatforms().contains(stage.getBoardingPlatform()));

        final List<StopCall> callingPoints = stage.getCallingPoints();
        assertEquals(firstCall, callingPoints.get(0).getStationId());
        assertEquals(OldTrafford.getId(), callingPoints.get(callingPoints.size()-1).getStationId());
        assertEquals(passedStops, stage.getPassedStopsCount());
    }

    private List<TransportStage<?, ?>> getStagesFor(TramTime queryTime, int numChanges, Station startStation, LatLong walkingDest) {
        JourneyRequest journeyRequest = new JourneyRequest(when, queryTime, false, numChanges,
                150, 1);

        Node startNode = graphQuery.getStationNode(txn, startStation);
        Node endNodeWalkNode = locationJourneyPlanner.createWalkingNode(txn, walkingDest, journeyRequest);

        List<StationWalk> walksToDest = locationJourneyPlanner.getStationWalks(walkingDest);

        List<Relationship> addedRelationships = new LinkedList<>();
        walksToDest.forEach(stationWalk -> addedRelationships.add(
                locationJourneyPlanner.createWalkRelationship(txn, endNodeWalkNode, stationWalk,
                TransportRelationshipTypes.WALKS_FROM)));

        Set<Station> destinationStations = new HashSet<>();
        walksToDest.forEach(stationWalk -> destinationStations.add(stationWalk.getStation()));

        Set<Long> destinationNodeIds = Collections.singleton(endNodeWalkNode.getId());

        List<RouteCalculator.TimedPath> timedPaths = getPathBetweenNodes(journeyRequest, startNode, destinationNodeIds,
                destinationStations, numChanges, queryTime);

        RouteCalculator.TimedPath timedPath = timedPaths.get(0);

        LowestCostsForRoutes lowestCostForRoutes = routeToRouteCosts.getLowestCostCalcutatorFor(destinationStations);
        final List<TransportStage<?, ?>> transportStages = pathToStages.mapDirect(txn, timedPath, journeyRequest, lowestCostForRoutes, destinationStations);

        locationJourneyPlanner.removeWalkNodeAndRelationships(addedRelationships, endNodeWalkNode);

        return transportStages;
    }

    private List<TransportStage<?, ?>> getStagesFor(TramTime queryTime, int numChanges, LatLong start, Station destination) {
        Set<Station> endStations = Collections.singleton(destination);
        JourneyRequest journeyRequest = new JourneyRequest(when, queryTime, false, numChanges, 150, 1);

        Node endNode = graphQuery.getStationNode(txn, destination);
        Node startOfWalkNode = locationJourneyPlanner.createWalkingNode(txn, start, journeyRequest);

        List<StationWalk> walks = locationJourneyPlanner.getStationWalks(start);
        List<Relationship> addedRelationships =  locationJourneyPlanner.createWalksToStations(txn, startOfWalkNode, walks);

        Set<Long> destinationNodeIds = Collections.singleton(endNode.getId());
        List<RouteCalculator.TimedPath> timedPaths = getPathBetweenNodes(journeyRequest, startOfWalkNode, destinationNodeIds,
            endStations, numChanges, queryTime);

        RouteCalculator.TimedPath timedPath = timedPaths.get(0);

        LowestCostsForRoutes lowestCostForRoutes = routeToRouteCosts.getLowestCostCalcutatorFor(endStations);
        final List<TransportStage<?, ?>> transportStages = pathToStages.mapDirect(txn, timedPath, journeyRequest, lowestCostForRoutes, endStations);

        locationJourneyPlanner.removeWalkNodeAndRelationships(addedRelationships, startOfWalkNode);

        return transportStages;
    }

    private List<TransportStage<?, ?>> getStagesFor(TramTime queryTime, int numChanges, Station startStation, Station destination) {
        Set<Station> endStations = Collections.singleton(destination);

        JourneyRequest journeyRequest = new JourneyRequest(when, queryTime, false, numChanges, 150, 1);

        List<RouteCalculator.TimedPath> timedPaths = getPathFor(startStation, destination, endStations, journeyRequest);
        assertFalse(timedPaths.isEmpty());
        RouteCalculator.TimedPath timedPath = timedPaths.get(0);

        LowestCostsForRoutes lowestCostForRoutes = routeToRouteCosts.getLowestCostCalcutatorFor(endStations);
        return pathToStages.mapDirect(txn, timedPath, journeyRequest, lowestCostForRoutes, endStations);
    }

    private List<RouteCalculator.TimedPath> getPathFor(Station startStation, Station destination, Set<Station> endStations,
                                                       JourneyRequest journeyRequest) {

        int numChanges = journeyRequest.getMaxChanges();
        TramTime queryTime = journeyRequest.getOriginalTime();
        Node startNode = graphQuery.getStationNode(txn, startStation);
        Node endNode = graphQuery.getStationNode(txn, destination);
        Set<Long> destinationNodeIds = Collections.singleton(endNode.getId());

        return getPathBetweenNodes(journeyRequest, startNode, destinationNodeIds, endStations, numChanges, queryTime);
    }

    private @NotNull List<RouteCalculator.TimedPath> getPathBetweenNodes(
            JourneyRequest journeyRequest, Node startNode, Set<Long> destinationNodeIds, Set<Station> endStations,
                                                                         int numChanges, TramTime queryTime) {
        PreviousVisits previous = new PreviousVisits();
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow);
        LowestCostsForRoutes lowestCostCalculator = routeToRouteCosts.getLowestCostCalcutatorFor(endStations);
        JourneyConstraints journeyConstraints = new JourneyConstraints(config, serviceRepository,
                journeyRequest, closedStationsRepository, endStations, lowestCostCalculator);
        ServiceHeuristics serviceHeuristics =  new ServiceHeuristics(stationRepository, nodeContentsRepository,
                journeyConstraints, queryTime, numChanges);

        LowestCostSeen lowestCostSeen = new LowestCostSeen();

        RouteCalculatorSupport.PathRequest pathRequest = new RouteCalculatorSupport.PathRequest(startNode, queryTime, numChanges,
                serviceHeuristics);

        Instant begin = Instant.now();
        final List<RouteCalculator.TimedPath> timedPaths = routeCalculator.findShortestPath(txn, destinationNodeIds, endStations,
                reasons, pathRequest, lowestCostCalculator, previous, lowestCostSeen, begin).collect(Collectors.toList());
        // Sort to give consistent test results, otherwise order is undefined
        return sorted(timedPaths);
    }

    private List<RouteCalculator.TimedPath> sorted(List<RouteCalculator.TimedPath> timedPaths) {
        return timedPaths.stream().sorted(Comparator.comparing(a -> getFirstTime(a.getPath()))).collect(Collectors.toList());
    }

    private TramTime getFirstTime(Path path) {
        for (Node node : path.nodes()) {
            if (node.hasLabel(GraphLabel.MINUTE)) {
                return GraphProps.getTime(node);
            }
        }
        return TramTime.of(23,59);
    }
}

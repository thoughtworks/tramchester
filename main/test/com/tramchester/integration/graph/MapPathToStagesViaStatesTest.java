package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphQuery;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.caches.PreviousSuccessfulVisits;
import com.tramchester.graph.search.*;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.ReachabilityRepository;
import com.tramchester.repository.ServiceRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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
    private ReachabilityRepository reachabilityRepository;
    private MapPathToStagesViaStates mapper;

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
        reachabilityRepository = componentContainer.get(ReachabilityRepository.class);
        graphQuery = componentContainer.get(GraphQuery.class);
        stationRepository = componentContainer.get(StationRepository.class);
        mapper = componentContainer.get(MapPathToStagesViaStates.class);
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldMapSimpleRoute() {
        TramTime queryTime = TramTime.of(9,15);
        int numChanges = 1;
        Station startStation = stationRepository.getStationById(TraffordBar.getId());
        Station destination = stationRepository.getStationById(TramStations.Altrincham.getId());

        List<TransportStage<?, ?>> result = getStagesFor(queryTime, numChanges, startStation, destination);

        assertFalse(result.isEmpty());
        TransportStage<?, ?> firstStage = result.get(0);
        assertEquals(startStation, firstStage.getFirstStation());
        assertEquals(destination, firstStage.getLastStation());
        assertEquals("Piccadilly - Altrincham", firstStage.getRoute().getName());
        assertEquals(TramTime.of(9,36), firstStage.getFirstDepartureTime());
        assertEquals(TramTime.of(9, 54), firstStage.getExpectedArrivalTime());
        assertEquals(18, firstStage.getDuration());
        assertTrue(firstStage.hasBoardingPlatform());
        assertTrue(startStation.getPlatforms().contains(firstStage.getBoardingPlatform()));

        final List<StopCall> callingPoints = firstStage.getCallingPoints();
        assertEquals(OldTrafford.getId(), callingPoints.get(0).getStationId());
        assertEquals(NavigationRoad.getId(), callingPoints.get(callingPoints.size()-1).getStationId());
        assertEquals(7, firstStage.getPassedStopsCount());
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

    private List<TransportStage<?, ?>> getStagesFor(TramTime queryTime, int numChanges, Station startStation, Station destination) {
        Set<Station> endStations = Collections.singleton(destination);

        JourneyRequest journeyRequest = new JourneyRequest(when, queryTime, false, numChanges, 150);

        List<RouteCalculator.TimedPath> timedPaths = getPathFor(startStation, destination, endStations, journeyRequest);
        assertFalse(timedPaths.isEmpty());
        RouteCalculator.TimedPath timedPath = timedPaths.get(0);

        return mapper.mapDirect(timedPath, journeyRequest, endStations);
    }

    private List<RouteCalculator.TimedPath> getPathFor(Station startStation, Station destination, Set<Station> endStations, JourneyRequest journeyRequest) {

        int numChanges = journeyRequest.getMaxChanges();
        TramTime queryTime = journeyRequest.getTime();
        Node startNode = graphQuery.getStationNode(txn, startStation);

        Node endNode = graphQuery.getStationNode(txn, destination);
        Set<Long> destinationNodeIds = Collections.singleton(endNode.getId());

        PreviousSuccessfulVisits previous = new PreviousSuccessfulVisits();
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow, numChanges);
        JourneyConstraints journeyConstraints = new JourneyConstraints(config, serviceRepository, journeyRequest, endStations);
        ServiceHeuristics serviceHeuristics =  new ServiceHeuristics(stationRepository, nodeContentsRepository, reachabilityRepository,
                journeyConstraints, queryTime, numChanges);
        RouteCalculatorSupport.PathRequest pathRequest = new RouteCalculatorSupport.PathRequest(startNode, queryTime, numChanges,
                serviceHeuristics);

        return routeCalculator.findShortestPath(txn, destinationNodeIds, endStations,
                reasons, pathRequest, previous).collect(Collectors.toList());
    }
}

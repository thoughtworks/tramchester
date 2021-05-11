package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
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

import static org.junit.jupiter.api.Assertions.assertFalse;

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
        Station startStation = stationRepository.getStationById(TramStations.TraffordBar.getId());
        Station destination = stationRepository.getStationById(TramStations.Altrincham.getId());

        Set<Station> endStations = Collections.singleton(destination);

        JourneyRequest journeyRequest = new JourneyRequest(when, queryTime, false, numChanges, 150);

        List<RouteCalculator.TimedPath> timedPaths = getPathFor(startStation, destination, endStations, journeyRequest);
        assertFalse(timedPaths.isEmpty());
        RouteCalculator.TimedPath timedPath = timedPaths.get(0);

        List<TransportStage<?, ?>> result = mapper.mapDirect(timedPath, journeyRequest, endStations);

        assertFalse(result.isEmpty());
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

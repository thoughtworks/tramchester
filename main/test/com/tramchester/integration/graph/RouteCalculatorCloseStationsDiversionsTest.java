package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.StationClosure;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.StationClosureForTest;
import com.tramchester.integration.testSupport.tram.IntegrationTramClosedStationsTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class RouteCalculatorCloseStationsDiversionsTest {
    // Note this needs to be > time for whole test fixture, see note below in @After
    private static final int TXN_TIMEOUT = 5*60;

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static IntegrationTramClosedStationsTestConfig config;

    private RouteCalculatorTestFacade calculator;
    private final static TramServiceDate when = new TramServiceDate(TestEnv.testDay());
    private Transaction txn;

    private final static List<StationClosure> closedStations = Collections.singletonList(
            new StationClosureForTest(TramStations.StPetersSquare, when.getDate(), when.getDate().plusWeeks(1)));

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new IntegrationTramClosedStationsTestConfig("closed_stpeters_int_test_tram.db", closedStations);
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
        database = componentContainer.get(GraphDatabase.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() throws IOException {
        componentContainer.close();
        TestEnv.deleteDBIfPresent(config);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        txn = database.beginTx(TXN_TIMEOUT, TimeUnit.SECONDS);
        StationRepository stationRepository = componentContainer.get(StationRepository.class);
        calculator = new RouteCalculatorTestFacade(componentContainer.get(RouteCalculator.class), stationRepository, txn);
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    private Set<TransportMode> getRequestedModes() {
        return Collections.emptySet();
    }

    @Test
    void shouldFindUnaffectedRouteNormally() {
        JourneyRequest journeyRequest = new JourneyRequest(when,TramTime.of(8,0), false,
                2, Duration.ofHours(2), 1, getRequestedModes());
        Set<Journey> result = calculator.calculateRouteAsSet(TramStations.Altrincham, TramStations.TraffordBar, journeyRequest);
        assertFalse(result.isEmpty());
    }

    @Test
    void shouldFindRouteWhenStartingFromClosedIfWalkPossible() {

        JourneyRequest journeyRequest = new JourneyRequest(when,TramTime.of(8,0), false,
                2, Duration.ofHours(2), 1, getRequestedModes());
        Set<Journey> results = calculator.calculateRouteAsSet(TramStations.StPetersSquare, TramStations.Altrincham,
                journeyRequest);
        assertFalse(results.isEmpty());
        results.forEach(result -> {
            final List<TransportStage<?, ?>> stages = result.getStages();
            assertEquals(2, stages.size(), "num stages " + result);
            assertEquals(TransportMode.Connect, stages.get(0).getMode(), "1st mode " + result);
            assertEquals(TransportMode.Tram, stages.get(1).getMode(), "2nd mode " + result);
        });
    }

    @Test
    void shouldFindRouteAroundCloseBackOnToTram() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                2, Duration.ofHours(2), 1, getRequestedModes());
        Set<Journey> results = calculator.calculateRouteAsSet(TramStations.Bury, TramStations.Altrincham,
                journeyRequest);
        assertFalse(results.isEmpty());
        results.forEach(result -> {
            final List<TransportStage<?, ?>> stages = result.getStages();
            assertEquals(4, stages.size(), "num stages " + result);
            assertEquals(TransportMode.Tram, stages.get(0).getMode(), "1st mode " + result);
            assertEquals(TransportMode.Connect, stages.get(1).getMode(), "2nd mode " + result);
            assertEquals(TransportMode.Connect, stages.get(2).getMode(), "3rd mode " + result);
            assertEquals(TransportMode.Tram, stages.get(3).getMode(), "4th mode " + result);
        });
    }

    @Test
    void shouldFindRouteToClosedStationViaWalkAtEnd() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8, 0),
                false, 3, Duration.ofHours(2), 1, getRequestedModes());
        Set<Journey> results = calculator.calculateRouteAsSet(TramStations.Bury, TramStations.StPetersSquare, journeyRequest);
        assertFalse(results.isEmpty());
        results.forEach(result -> {
            final List<TransportStage<?, ?>> stages = result.getStages();
            assertEquals(2, stages.size(), "num stages " + result);
            assertEquals(TransportMode.Tram, stages.get(0).getMode(), "1st mode " + result);
            assertEquals(TransportMode.Connect, stages.get(1).getMode(), "2nd mode " + result);
        });
    }

}

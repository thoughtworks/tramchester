package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.StationClosures;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.StationClosuresForTest;
import com.tramchester.integration.testSupport.tram.IntegrationTramClosedStationsTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.TramsOnly;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteCalculatorCloseStationsTest {
    // Note this needs to be > time for whole test fixture, see note below in @After
    private static final int TXN_TIMEOUT = 5*60;

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;

    private RouteCalculatorTestFacade calculator;
    private final static TramDate when = TestEnv.testDay();
    private Transaction txn;

    // see note below on DB deletion
    private final static List<StationClosures> closedStations = Arrays.asList(
            new StationClosuresForTest(Shudehill, when, when.plusWeeks(1), true),
            new StationClosuresForTest(PiccadillyGardens, when, when.plusWeeks(1), false));

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {
        TramchesterConfig config = new IntegrationTramClosedStationsTestConfig(closedStations, true);

        // if above closedStation list is changed need to enable this once
        //TestEnv.deleteDBIfPresent(config);

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
        StationRepository stationRepository = componentContainer.get(StationRepository.class);
        calculator = new RouteCalculatorTestFacade(componentContainer.get(RouteCalculator.class), stationRepository, txn);
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldFindUnaffectedRouteNormally() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                2, Duration.ofMinutes(120), 1, getRequestedModes());
        Set<Journey> result = calculator.calculateRouteAsSet(TramStations.Altrincham, TraffordBar, journeyRequest);
        assertFalse(result.isEmpty());
    }

    @Test
    void shouldHandlePartialClosure() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                3, Duration.ofMinutes(120), 1, getRequestedModes());
        Set<Journey> result = calculator.calculateRouteAsSet(Piccadilly, StPetersSquare, journeyRequest);
        assertFalse(result.isEmpty());
    }

    private Set<TransportMode> getRequestedModes() {
        return TramsOnly;
    }

    @Test
    void shouldFindRouteAroundClosedStation() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                3, Duration.ofMinutes(120), 1, getRequestedModes());
        Set<Journey> result = calculator.calculateRouteAsSet(MarketStreet, Victoria, journeyRequest);
        assertFalse(result.isEmpty());
    }

    @Test
    void shouldNotFindRouteToClosedStationViaDirectTram() {
        Set<Journey> singleStage = getSingleStageBuryToEccles(when);
        assertTrue(singleStage.isEmpty());
    }

    @Test
    void shouldFindRouteToClosedStationViaDirectTramWhenAfterClosurePeriod() {
        TramDate travelDate = TestEnv.avoidChristmasDate(when.plusDays(10));

        Set<Journey> singleStage = getSingleStageBuryToEccles(travelDate);
        assertFalse(singleStage.isEmpty());
    }

    @NotNull
    private Set<Journey> getSingleStageBuryToEccles(TramDate travelDate) {
        JourneyRequest journeyRequest = new JourneyRequest(travelDate, TramTime.of(8, 0),
                false, 0, Duration.ofMinutes(120), 1, getRequestedModes());

        Set<Journey> journeys = calculator.calculateRouteAsSet(Bury, Shudehill, journeyRequest);
        return journeys.stream().filter(results -> results.getStages().size() == 1).collect(Collectors.toSet());
    }

}

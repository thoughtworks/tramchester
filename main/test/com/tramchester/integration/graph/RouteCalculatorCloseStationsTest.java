package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.StationClosure;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class RouteCalculatorCloseStationsTest {
    // Note this needs to be > time for whole test fixture, see note below in @After
    private static final int TXN_TIMEOUT = 5*60;

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;

    private RouteCalculatorTestFacade calculator;
    private final LocalDate when = TestEnv.testDay();
    private Transaction txn;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig config = new ClosedStationsTramTestConfig();
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
        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(when),TramTime.of(8,0), false,
                2, 120, 1);
        Set<Journey> result = calculator.calculateRouteAsSet(TramStations.Altrincham, TramStations.TraffordBar, journeyRequest);
        assertFalse(result.isEmpty());
    }
    
    @Test
    void shouldFindRouteAroundClosedStation() {
        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(when),TramTime.of(8,0), false,
                2, 120, 1);
        Set<Journey> result = calculator.calculateRouteAsSet(TramStations.PiccadillyGardens, TramStations.Victoria,
                journeyRequest);
        assertFalse(result.isEmpty());
    }

    @Test
    void shouldNotFindRouteToClosedStationViaDirectTram() {
        Set<Journey> singleStage = getSingleStageBuryToEccles(when);
        assertTrue(singleStage.isEmpty());
    }

    @Disabled("wip - check in")
    @Test
    void shouldFindRouteWhenStartingFromClosedIfWalkPossible() {
        fail("todo");
    }

    @Disabled("wip - check in")
    @Test
    void shouldFindRouteAroundCloseBackOnToTram() {
        fail("todo");
    }

    @Test
    void shouldFindRouteToClosedStationViaDirectTramWhenAfterClosure() {
        Set<Journey> singleStage = getSingleStageBuryToEccles(when.plusDays(10));
        assertFalse(singleStage.isEmpty());
    }

    @NotNull
    private Set<Journey> getSingleStageBuryToEccles(LocalDate travelDate) {
        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(travelDate), TramTime.of(8, 0),
                false, 0, 120, 1);
        Set<Journey> journeys = calculator.calculateRouteAsSet(TramStations.Bury, TramStations.Shudehill, journeyRequest);
        return journeys.stream().filter(results -> results.getStages().size() == 1).collect(Collectors.toSet());
    }

    @Test
    void shouldFindRouteToClosedStationViaWalkWithChanges() {
        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(when), TramTime.of(8, 0),
                false, 3, 120, 1);
        Set<Journey> journeys = calculator.calculateRouteAsSet(TramStations.Bury, TramStations.Shudehill, journeyRequest);
        assertFalse(journeys.isEmpty());
    }

    private static class ClosedStationsTramTestConfig extends IntegrationTramTestConfig {

        private final static List<StationClosure> closedStations = Collections.singletonList(
                new StationClosureForTest(TramStations.Shudehill, TestEnv.testDay()));

        public ClosedStationsTramTestConfig() {
            super("closed_shudehill_int_test_tram.db", closedStations);
        }

    }

    private static class StationClosureForTest implements StationClosure {

        private final TramStations station;
        private final LocalDate begin;

        private StationClosureForTest(TramStations station, LocalDate begin) {
            this.station = station;
            this.begin = begin;
        }

        @Override
        public IdSet<Station> getStations() {
            return IdSet.singleton(station.getId());
        }

        @Override
        public LocalDate getBegin() {
            return begin;
        }

        @Override
        public LocalDate getEnd() {
            return getBegin().plusWeeks(1);
        }
    }
}

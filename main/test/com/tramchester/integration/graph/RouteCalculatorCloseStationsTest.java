package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.Journey;
import com.tramchester.domain.StationClosure;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteCalculatorCloseStationsTest {
    // Note this needs to be > time for whole test fixture, see note below in @After
    private static final int TXN_TIMEOUT = 5*60;

    private static Dependencies dependencies;
    private static GraphDatabase database;
    private static TramchesterConfig config;

    private RouteCalculator calculator;
    private final LocalDate when = TestEnv.testDay();
    private Transaction txn;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        config = new ClosedStationsTramTestConfig();
        dependencies.initialise(config);
        database = dependencies.get(GraphDatabase.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        txn = database.beginTx(TXN_TIMEOUT, TimeUnit.SECONDS);
        calculator = dependencies.get(RouteCalculator.class);
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldFindUnaffectedRouteNormally() {
        RouteCalculatorTest.validateAtLeastNJourney(calculator,1, txn,
                Stations.Altrincham, Stations.TraffordBar, TramTime.of(8,0), when, 2, 120 );
    }
    
    @Test
    void shouldFindRouteAroundClosedStation() {
        RouteCalculatorTest.validateAtLeastNJourney(calculator,1, txn,
                Stations.PiccadillyGardens, Stations.Victoria, TramTime.of(8,0), when,
                2, 120 );
    }

    @Test
    void shouldNotFindRouteToClosedStation() {
        Stream<Journey> journeyStream = calculator.calculateRoute(txn, Stations.Bury, Stations.Shudehill,
                new JourneyRequest(new TramServiceDate(when), TramTime.of(8,0),
                false, 1, 120));

        Set<Journey> journeys = journeyStream.limit(1).collect(Collectors.toSet());
        journeyStream.close();

        assertTrue(journeys.isEmpty());
    }

    private static class ClosedStationsTramTestConfig extends IntegrationTramTestConfig {

        @Override
        public List<StationClosure> getStationClosures() {
            return closedStations;
        }

        private final List<StationClosure> closedStations = Collections.singletonList(
                new StationClosureForTest(Stations.Shudehill));
    }

    private static class StationClosureForTest implements StationClosure {

        private final Station station;

        private StationClosureForTest(Station station) {
            this.station = station;
        }

        @Override
        public IdFor<Station> getStation() {
            return station.getId();
        }

        @Override
        public LocalDate getBegin() {
            return TestEnv.testDay();
        }

        @Override
        public LocalDate getEnd() {
            return getBegin().plusWeeks(1);
        }
    }
}

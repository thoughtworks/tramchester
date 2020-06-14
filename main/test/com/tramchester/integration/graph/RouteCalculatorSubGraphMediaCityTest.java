package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.DiagramCreator;
import com.tramchester.domain.Journey;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.ActiveGraphFilter;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.testSupport.RoutesForTesting;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

class RouteCalculatorSubGraphMediaCityTest {
    private static Dependencies dependencies;
    private static GraphDatabase database;

    private RouteCalculator calculator;
    private final LocalDate nextTuesday = TestEnv.nextTuesday(0);

    private static final List<Station> stations = Arrays.asList(
            Stations.ExchangeSquare,
            Stations.StPetersSquare,
            Stations.Deansgate,
            Stations.Cornbrook,
            Stations.Pomona,
            Stations.ExchangeQuay,
            Stations.SalfordQuay,
            Stations.Anchorage,
//            "9400ZZMAEXC", // Exchange Quay
//            "9400ZZMASQY", // Salford Quays
//            "9400ZZMAANC", // Anchorage
            Stations.HarbourCity,
            Stations.MediaCityUK,
            Stations.TraffordBar);
    private Transaction txn;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {
        ActiveGraphFilter graphFilter = new ActiveGraphFilter();
        graphFilter.addRoute(RoutesForTesting.ASH_TO_ECCLES);
        graphFilter.addRoute(RoutesForTesting.ROCH_TO_DIDS);
        graphFilter.addRoute(RoutesForTesting.ECCLES_TO_ASH);
        graphFilter.addRoute(RoutesForTesting.DIDS_TO_ROCH);
        stations.forEach(graphFilter::addStation);

        dependencies = new Dependencies(graphFilter);
        dependencies.initialise(new SubgraphConfig());

        database = dependencies.get(GraphDatabase.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        calculator = dependencies.get(RouteCalculator.class);
        txn = database.beginTx();
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @SuppressWarnings("JUnitTestMethodWithNoAssertions")
    @Test
    void shouldHaveMediaCityToExchangeSquare() {
        validateAtLeastOneJourney(Stations.MediaCityUK, Stations.Cornbrook, TramTime.of(9,0), TestEnv.nextSaturday());
        validateAtLeastOneJourney(Stations.MediaCityUK, Stations.ExchangeSquare, TramTime.of(9,0), TestEnv.nextSaturday());
        validateAtLeastOneJourney(Stations.MediaCityUK, Stations.ExchangeSquare, TramTime.of(9,0), TestEnv.nextSunday());
    }

    @Test
    void shouldHaveJourneyFromEveryStationToEveryOther() {
        List<String> failures = new LinkedList<>();

        for (Station start: stations) {
            for (Station destination: stations) {
                if (!start.equals(destination)) {
                    for (int i = 0; i < 7; i++) {
                        LocalDate day = nextTuesday.plusDays(i);
                        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(day), TramTime.of(9,0), false);
                        Set<Journey> journeys = calculator.calculateRoute(txn, start, destination, journeyRequest).collect(Collectors.toSet());
                        if (journeys.isEmpty()) {
                            failures.add(day.getDayOfWeek() +": "+start+"->"+destination);
                        }
                    }
                }
            }
        }
        Assertions.assertTrue(failures.isEmpty());
    }

    @SuppressWarnings("JUnitTestMethodWithNoAssertions")
    @Test
    void reproduceMediaCityIssue() {
        validateAtLeastOneJourney(Stations.ExchangeSquare, Stations.MediaCityUK, TramTime.of(12,0), nextTuesday);
    }

    @SuppressWarnings("JUnitTestMethodWithNoAssertions")
    @Test
    void reproduceMediaCityIssueSaturdays() {
        validateAtLeastOneJourney(Stations.ExchangeSquare, Stations.MediaCityUK, TramTime.of(9,0), TestEnv.nextSaturday());
    }

    @Test
    void shouldHaveSimpleJourney() {
        Set<Journey> results = calculator.calculateRoute(txn, Stations.Pomona, Stations.MediaCityUK,
                new JourneyRequest(new TramServiceDate(nextTuesday), TramTime.of(12, 0), false)).collect(Collectors.toSet());
        Assertions.assertTrue(results.size()>0);
    }

    @Disabled
    @Test
    void produceDiagramOfGraphSubset() throws IOException {
        DiagramCreator creator = new DiagramCreator(database, 5);
        List<String> toDraw = new ArrayList<>();
        toDraw.add(Stations.MediaCityUK.getId());

        creator.create(format("%s_trams.dot", "subgraph_mediacity"), toDraw);
    }

    private static class SubgraphConfig extends IntegrationTramTestConfig {
        public SubgraphConfig() {
            super("sub_mediacity_tramchester.db");
        }

        @Override
        public boolean getRebuildGraph() {
            return true;
        }
    }

    private void validateAtLeastOneJourney(Station start, Station dest, TramTime time, LocalDate date) {
        RouteCalculatorTest.validateAtLeastOneJourney(calculator, txn, start, dest, time, date, 5);
    }
}

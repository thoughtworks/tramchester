package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.DiagramCreator;
import com.tramchester.domain.Journey;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.graphbuild.ActiveGraphFilter;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.*;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.TestEnv.DAYS_AHEAD;
import static java.lang.String.format;

class RouteCalculatorSubGraphMediaCityTest {
    private static Dependencies dependencies;
    private static GraphDatabase database;
    private static SubgraphConfig config;

    private RouteCalculator calculator;
    private final LocalDate when = TestEnv.testDay();

    private static final List<TramStations> stations = Arrays.asList(
            TramStations.ExchangeSquare,
            TramStations.StPetersSquare,
            TramStations.Deansgate,
            TramStations.Cornbrook,
            TramStations.Pomona,
            TramStations.ExchangeQuay,
            TramStations.SalfordQuay,
            TramStations.Anchorage,
            TramStations.HarbourCity,
            TramStations.MediaCityUK,
            TramStations.TraffordBar);
    private Transaction txn;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        ActiveGraphFilter graphFilter = new ActiveGraphFilter();
        graphFilter.addRoute(RoutesForTesting.ASH_TO_ECCLES);
        graphFilter.addRoute(RoutesForTesting.ROCH_TO_DIDS);
        graphFilter.addRoute(RoutesForTesting.ECCLES_TO_ASH);
        graphFilter.addRoute(RoutesForTesting.DIDS_TO_ROCH);
        stations.forEach(TramStations::getId);

        dependencies = new Dependencies(graphFilter);
        config = new SubgraphConfig();
        dependencies.initialise(config);

        database = dependencies.get(GraphDatabase.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        calculator = dependencies.get(RouteCalculator.class);
        stationRepository = dependencies.get(StationRepository.class);
        txn = database.beginTx();
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @SuppressWarnings("JUnitTestMethodWithNoAssertions")
    @Test
    void shouldHaveMediaCityToExchangeSquare() {
        validateAtLeastOneJourney(TramStations.MediaCityUK, TramStations.Cornbrook, TramTime.of(9,0), TestEnv.nextSaturday());
        validateAtLeastOneJourney(TramStations.MediaCityUK, TramStations.ExchangeSquare, TramTime.of(9,0), TestEnv.nextSaturday());
        validateAtLeastOneJourney(TramStations.MediaCityUK, TramStations.ExchangeSquare, TramTime.of(9,0), TestEnv.nextSunday());
    }

    @DataExpiryCategory
    @Test
    void shouldHaveJourneyFromEveryStationToEveryOtherNDaysAhead() {
        List<String> failures = new LinkedList<>();

        for (TramStations start: stations) {
            for (TramStations destination: stations) {
                if (!start.equals(destination)) {
                    for (int i = 0; i < DAYS_AHEAD; i++) {
                        LocalDate day = when.plusDays(i);
                        JourneyRequest journeyRequest =
                                new JourneyRequest(new TramServiceDate(day), TramTime.of(9,0), false, 3, config.getMaxJourneyDuration());
                        Set<Journey> journeys = calculator.calculateRoute(txn, TramStations.real(stationRepository,start),
                                TramStations.real(stationRepository, destination), journeyRequest).collect(Collectors.toSet());
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
        validateAtLeastOneJourney(TramStations.ExchangeSquare, TramStations.MediaCityUK, TramTime.of(12,0), when);
    }

    @SuppressWarnings("JUnitTestMethodWithNoAssertions")
    @Test
    void reproduceMediaCityIssueSaturdays() {
        validateAtLeastOneJourney(TramStations.ExchangeSquare, TramStations.MediaCityUK, TramTime.of(9,0), TestEnv.nextSaturday());
    }

    @Test
    void shouldHaveSimpleJourney() {
        Set<Journey> results = calculator.calculateRoute(txn, TramStations.real(stationRepository, TramStations.Pomona),
                TramStations.real(stationRepository, TramStations.MediaCityUK),
                new JourneyRequest(new TramServiceDate(when), TramTime.of(12, 0), false, 3, config.getMaxJourneyDuration())).collect(Collectors.toSet());
        Assertions.assertTrue(results.size()>0);
    }

    //@Disabled
    @Test
    void produceDiagramOfGraphSubset() throws IOException {
        DiagramCreator creator = new DiagramCreator(database, 3);
        creator.create(format("%s_trams.dot", "subgraph_mediacity"), Stations.MediaCityUK);
    }

    private static class SubgraphConfig extends IntegrationTramTestConfig {
        public SubgraphConfig() {
            super("sub_mediacity_tramchester.db");
        }
    }

    private void validateAtLeastOneJourney(TramStations start, TramStations dest, TramTime time, LocalDate date) {
        RouteCalculatorTest.validateAtLeastNJourney(calculator, stationRepository, 1, txn, start, dest, time, date, 5, config.getMaxJourneyDuration());
    }
}

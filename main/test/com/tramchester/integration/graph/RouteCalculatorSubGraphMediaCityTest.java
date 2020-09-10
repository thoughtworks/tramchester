package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.DiagramCreator;
import com.tramchester.domain.Journey;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.graphbuild.ActiveGraphFilter;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.*;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static com.tramchester.testSupport.TestEnv.DAYS_AHEAD;
import static com.tramchester.testSupport.TramStations.MediaCityUK;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RouteCalculatorSubGraphMediaCityTest {
    private static Dependencies dependencies;
    private static GraphDatabase database;
    private static SubgraphConfig config;

    private RouteCalculatorTestFacade calculator;
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
            MediaCityUK,
            TramStations.TraffordBar);
    private Transaction txn;

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
        StationRepository stationRepository = dependencies.get(StationRepository.class);
        txn = database.beginTx();
        calculator = new RouteCalculatorTestFacade(dependencies.get(RouteCalculator.class), stationRepository, txn);
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldHaveMediaCityToExchangeSquare() {
        validateAtLeastOneJourney(MediaCityUK, TramStations.Cornbrook, TramTime.of(9,0), TestEnv.nextSaturday());
        validateAtLeastOneJourney(MediaCityUK, TramStations.ExchangeSquare, TramTime.of(9,0), TestEnv.nextSaturday());
        validateAtLeastOneJourney(MediaCityUK, TramStations.ExchangeSquare, TramTime.of(9,0), TestEnv.nextSunday());
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
                                new JourneyRequest(new TramServiceDate(day), TramTime.of(9,0), false,
                                        3, config.getMaxJourneyDuration());
                        Set<Journey> journeys = calculator.calculateRouteAsSet(start, destination, journeyRequest);
                        if (journeys.isEmpty()) {
                            failures.add(day.getDayOfWeek() +": "+start+"->"+destination);
                        }
                    }
                }
            }
        }
        Assertions.assertTrue(failures.isEmpty());
    }

    @Test
    void reproduceMediaCityIssue() {
        validateAtLeastOneJourney(TramStations.ExchangeSquare, MediaCityUK, TramTime.of(12,0), when);
    }

    @Test
    void reproduceMediaCityIssueSaturdays() {
        validateAtLeastOneJourney(TramStations.ExchangeSquare, MediaCityUK, TramTime.of(9,0), TestEnv.nextSaturday());
    }

    @Test
    void shouldHaveSimpleJourney() {
        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(when), TramTime.of(12, 0), false, 3,
                config.getMaxJourneyDuration());
        Set<Journey> results = calculator.calculateRouteAsSet(TramStations.Pomona, MediaCityUK, journeyRequest);
        Assertions.assertTrue(results.size()>0);
    }

    //@Disabled
    @Test
    void produceDiagramOfGraphSubset() throws IOException {
        DiagramCreator creator = new DiagramCreator(database, 3);
        creator.create(format("%s_trams.dot", "subgraph_mediacity"), TramStations.of(MediaCityUK));
    }

    private static class SubgraphConfig extends IntegrationTramTestConfig {
        public SubgraphConfig() {
            super("sub_mediacity_tramchester.db");
        }
    }

    private void validateAtLeastOneJourney(TramStations start, TramStations dest, TramTime time, LocalDate date) {
        JourneyRequest journeyRequest = new JourneyRequest(date, time, false, 5, config.getMaxJourneyDuration());
        Set<Journey> results = calculator.calculateRouteAsSet(start, dest, journeyRequest, 1);
        assertFalse(results.isEmpty());
    }
}

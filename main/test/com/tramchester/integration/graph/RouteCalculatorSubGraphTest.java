package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.DiagramCreator;
import com.tramchester.domain.Journey;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.graphbuild.ActiveGraphFilter;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.testSupport.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.RouteCalculatorTestFacade;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramStations;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.tramchester.testSupport.TramStations.Cornbrook;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RouteCalculatorSubGraphTest {
    private static Dependencies dependencies;
    private static GraphDatabase database;
    private static SubgraphConfig config;

    private RouteCalculatorTestFacade calculator;
    private final LocalDate when = TestEnv.testDay();
    private static final List<TramStations> stations = Arrays.asList(
            Cornbrook,
            TramStations.StPetersSquare,
            TramStations.Deansgate,
            TramStations.Pomona);
    private Transaction txn;
    private TramTime tramTime;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        ActiveGraphFilter graphFilter = new ActiveGraphFilter();
//        graphFilter.addRoute(RouteCodesForTesting.ALTY_TO_BURY);

        stations.forEach(station->graphFilter.addStation(station.getId()));

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

        tramTime = TramTime.of(8, 0);
    }

    @AfterEach
    void onceAfterEveryTest() {
        txn.close();
    }

    @Test
    void reproduceIssueEdgePerTrip() {

        validateAtLeastOneJourney(TramStations.StPetersSquare, TramStations.Deansgate, TramTime.of(19,51), when);
        validateAtLeastOneJourney(Cornbrook, TramStations.Pomona, TramTime.of(19,51).plusMinutes(6), when);

        validateAtLeastOneJourney(TramStations.Deansgate, Cornbrook, TramTime.of(19,51).plusMinutes(3), when);
        validateAtLeastOneJourney(TramStations.Deansgate, TramStations.Pomona, TramTime.of(19,51).plusMinutes(3), when);

        validateAtLeastOneJourney(TramStations.StPetersSquare, TramStations.Pomona, TramTime.of(19,51), when);
        validateAtLeastOneJourney(TramStations.StPetersSquare, TramStations.Pomona, TramTime.of(19,56), when);
    }

    @Test
    void shouldHandleCrossingMidnightDirect() {
        validateAtLeastOneJourney(Cornbrook, TramStations.StPetersSquare, TramTime.of(23,55), when);
    }

    @SuppressWarnings("JUnitTestMethodWithNoAssertions")
    @Test
    void shouldHaveJourneysBetweenAllStations() {
        for (TramStations start: stations) {
            for (TramStations destination: stations) {
                if (!start.equals(destination)) {
                    validateAtLeastOneJourney(start, destination, tramTime, when);
                }
            }
        }
    }

    @Test
    void shouldHaveSimpleOneStopJourney() {
        Set<Journey> results = getJourneys(Cornbrook, TramStations.Pomona, when);
        Assertions.assertTrue(results.size()>0);
    }

    @Test
    void shouldHaveSimpleOneStopJourneyAtWeekend() {
        Set<Journey> results = getJourneys(Cornbrook, TramStations.Pomona, TestEnv.nextSaturday());
        Assertions.assertTrue(results.size()>0);
    }

    @Test
    void shouldHaveSimpleOneStopJourneyBetweenInterchanges() {
        Set<Journey> results = getJourneys(TramStations.StPetersSquare, TramStations.Deansgate, when);
        Assertions.assertTrue(results.size()>0);
    }

    @Test
    void shouldHaveSimpleJourney() {
        Set<Journey> results = getJourneys(TramStations.StPetersSquare, Cornbrook, when);
        Assertions.assertTrue(results.size()>0);
    }

    @Test
    void produceDiagramOfGraphSubset() throws IOException {
        DiagramCreator creator = new DiagramCreator(database, 7);
        creator.create(format("%s_trams.dot", "subgraph"), TramStations.of(Cornbrook));
    }

    private static class SubgraphConfig extends IntegrationTramTestConfig {
        public SubgraphConfig() {
            super("subgraph_tramchester.db");
        }

    }

    @NotNull
    private Set<Journey> getJourneys(TramStations start, TramStations destination, LocalDate when) {
        JourneyRequest journeyRequest = new JourneyRequest(when, tramTime, false, 3,
                config.getMaxJourneyDuration());
        return calculator.calculateRouteAsSet(start,destination, journeyRequest);
    }

    private void validateAtLeastOneJourney(TramStations start, TramStations dest, TramTime time, LocalDate date) {
        JourneyRequest journeyRequest = new JourneyRequest(when, tramTime, false, 5,
                config.getMaxJourneyDuration());

        Set<Journey> results = calculator.calculateRouteAsSet(start, dest, journeyRequest, 1);
        assertFalse(results.isEmpty());
    }
}

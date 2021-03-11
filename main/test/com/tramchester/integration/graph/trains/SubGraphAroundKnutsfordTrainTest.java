package com.tramchester.integration.graph.trains;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.DiagramCreator;
import com.tramchester.domain.Journey;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.graphbuild.ActiveGraphFilter;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.graph.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.IntegrationTrainTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TestStations;
import com.tramchester.testSupport.reference.TrainStations;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.tramchester.testSupport.reference.TrainStations.Hale;
import static com.tramchester.testSupport.reference.TrainStations.Knutsford;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class SubGraphAroundKnutsfordTrainTest {
    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static SubgraphConfig config;

    private RouteCalculatorTestFacade calculator;
    private final LocalDate when = TestEnv.testDay();
    private static final List<TrainStations> stations = Arrays.asList(
            Hale,
            TrainStations.Ashley,
            TrainStations.Mobberley,
            Knutsford);
    private Transaction txn;
    private TramTime tramTime;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {
        config = new SubgraphConfig();
        TestEnv.deleteDBIfPresent(config);

        ActiveGraphFilter graphFilter = new ActiveGraphFilter();
        stations.forEach(station -> graphFilter.addStation(station.getId()));

        componentContainer = new ComponentsBuilder<>().setGraphFilter(graphFilter).create(config, TestEnv.NoopRegisterMetrics());
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
        StationRepository stationRepository = componentContainer.get(StationRepository.class);
        txn = database.beginTx();
        calculator = new RouteCalculatorTestFacade(componentContainer.get(RouteCalculator.class), stationRepository, txn);

        tramTime = TramTime.of(8, 0);
    }

    @AfterEach
    void onceAfterEveryTest() {
        txn.close();
    }

    @Test
    void shouldHandleCrossingMidnightDirect() {
        validateAtLeastOneJourney(Knutsford, Hale, TramTime.of(23,55), when);
    }

    @SuppressWarnings("JUnitTestMethodWithNoAssertions")
    @Test
    void shouldHaveJourneysBetweenAllStations() {
        for (TrainStations start: stations) {
            for (TrainStations destination: stations) {
                if (!start.equals(destination)) {
                    validateAtLeastOneJourney(start, destination, tramTime, when);
                }
            }
        }
    }

    @Test
    void shouldHaveSimpleJourney() {
        Set<Journey> results = getJourneys(Hale, Knutsford, when);
        Assertions.assertTrue(results.size()>0);
    }

    @Test
    void produceDiagramOfGraphSubset() throws IOException {
        DiagramCreator creator = componentContainer.get(DiagramCreator.class);
        creator.create(format("%s_trains.dot", "around_hale"), TrainStations.of(Hale), Integer.MAX_VALUE);
    }

    private static class SubgraphConfig extends IntegrationTrainTestConfig {
        public SubgraphConfig() {
            super("subgraph_hale_trains_tramchester.db");
        }
    }

    @NotNull
    private Set<Journey> getJourneys(TestStations start, TestStations destination, LocalDate when) {
        JourneyRequest journeyRequest = new JourneyRequest(when, tramTime, false, 3,
                config.getMaxJourneyDuration());
        return calculator.calculateRouteAsSet(start,destination, journeyRequest);
    }

    private void validateAtLeastOneJourney(TestStations start, TestStations dest, TramTime time, LocalDate date) {
        JourneyRequest journeyRequest = new JourneyRequest(when, tramTime, false, 5,
                config.getMaxJourneyDuration());

        Set<Journey> results = calculator.calculateRouteAsSet(start, dest, journeyRequest, 1);
        assertFalse(results.isEmpty());
    }
}

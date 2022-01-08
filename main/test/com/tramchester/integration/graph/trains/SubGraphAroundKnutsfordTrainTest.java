package com.tramchester.integration.graph.trains;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.DiagramCreator;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.filters.ConfigurableGraphFilter;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.TrainTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TrainTest
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class SubGraphAroundKnutsfordTrainTest {
    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static SubgraphConfig config;

    private RouteCalculatorTestFacade testFacade;
    private final LocalDate when = TestEnv.testDay();

    private static final List<RailStationIds> stations = Arrays.asList(RailStationIds.Hale,
            RailStationIds.Ashley, RailStationIds.Mobberley, RailStationIds.Knutsford);

    private Transaction txn;
    private TramTime tramTime;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {
        config = new SubgraphConfig();
        TestEnv.deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder().
                configureGraphFilter(SubGraphAroundKnutsfordTrainTest::configureGraphFilter).
                create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        database = componentContainer.get(GraphDatabase.class);
    }

    private static void configureGraphFilter(ConfigurableGraphFilter graphFilter) {
        stations.forEach(station -> graphFilter.addStation(station.getId()));
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() throws IOException {
        componentContainer.close();
        TestEnv.deleteDBIfPresent(config);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        stationRepository = componentContainer.get(StationRepository.class);
        RouteCalculator routeCalculator = componentContainer.get(RouteCalculator.class);

        txn = database.beginTx();
        testFacade = new RouteCalculatorTestFacade(routeCalculator, stationRepository, txn);

        tramTime = TramTime.of(9, 0);
    }

    @AfterEach
    void onceAfterEveryTest() {
        txn.close();
    }

    @Test
    void shouldValidateFilterStationsAreValid() {
        stations.forEach(station -> assertNotNull(stationRepository.getStationById(station.getId())));
    }

    @Test
    void shouldHaveKnutsfordToAndFromHale() {
        validateAtLeastOneJourney(RailStationIds.Hale, RailStationIds.Knutsford);
        validateAtLeastOneJourney(RailStationIds.Knutsford, RailStationIds.Hale);
    }

    @SuppressWarnings("JUnitTestMethodWithNoAssertions")
    @Test
    void shouldHaveJourneysBetweenAllStations() {
        for (RailStationIds start: stations) {
            for (RailStationIds destination: stations) {
                if (!start.equals(destination)) {
                    validateAtLeastOneJourney(start, destination);
                }
            }
        }
    }

    @Test
    void shouldHaveSimpleJourney() {
        JourneyRequest journeyRequest = new JourneyRequest(when, tramTime, false, 0,
                30, 1);
        Set<Journey> results = testFacade.calculateRouteAsSet(RailStationIds.Hale.getId(), RailStationIds.Knutsford.getId(), journeyRequest);
        Assertions.assertTrue(results.size()>0);
    }

    @Test
    void produceDiagramOfGraphSubset() throws IOException {
        DiagramCreator creator = componentContainer.get(DiagramCreator.class);
        final Station station = stationRepository.getStationById(RailStationIds.Hale.getId());
        assertNotNull(station);
        creator.create(Path.of(format("%s_trains.dot", "around_hale")), station, 100, false);
    }

    private void validateAtLeastOneJourney(RailStationIds start, RailStationIds dest) {
        JourneyRequest journeyRequest = new JourneyRequest(when, tramTime, false, 0,
                30, 1);

        Set<Journey> results = testFacade.calculateRouteAsSet(start.getId(), dest.getId(), journeyRequest);
        assertFalse(results.isEmpty(), "No results from " + start + " to " + dest);
    }

    private static class SubgraphConfig extends IntegrationRailTestConfig {
        public SubgraphConfig() {
            super("subgraph_hale_trains_tramchester.db");
        }
    }
}

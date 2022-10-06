package com.tramchester.integration.graph.rail;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.DiagramCreator;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.NumberOfChanges;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.filters.ConfigurableGraphFilter;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.graph.search.routes.RouteToRouteCosts;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.TrainTest;
import org.checkerframework.checker.units.qual.C;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.tramchester.domain.reference.TransportMode.Train;
import static com.tramchester.integration.testSupport.rail.RailStationIds.Hale;
import static com.tramchester.integration.testSupport.rail.RailStationIds.Knutsford;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;

@TrainTest
class SubGraphAroundKnutsfordRailTest {
    public static final int INITIAL_WAIT = 60;
    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static SubgraphConfig config;

    private RouteCalculatorTestFacade testFacade;
    private final TramDate when = TestEnv.testDay();

    private static final List<RailStationIds> stations = Arrays.asList(Hale,
            RailStationIds.Ashley, RailStationIds.Mobberley, Knutsford);

    private Transaction txn;
    private TramTime tramTime;
    private StationRepository stationRepository;
    private RouteToRouteCosts routeToRouteCosts;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {
        config = new SubgraphConfig();

        TestEnv.deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder().
                configureGraphFilter(SubGraphAroundKnutsfordRailTest::configureGraphFilter).
                create(config, TestEnv.NoopRegisterMetrics());

        componentContainer.initialise();

        database = componentContainer.get(GraphDatabase.class);
    }

    private static void configureGraphFilter(ConfigurableGraphFilter graphFilter, TransportData transportData) {
        stations.forEach(station -> graphFilter.addStation(station.getId()));
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() throws IOException {
        componentContainer.close();
        // should be no caching for sub-graphs
        TestEnv.deleteDBIfPresent(config);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        stationRepository = componentContainer.get(StationRepository.class);
        routeToRouteCosts = componentContainer.get(RouteToRouteCosts.class);

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
        Station hale = Hale.from(stationRepository);
        Station knutsford = Knutsford.from(stationRepository);
        TimeRange timeRange = TimeRange.of(tramTime, tramTime.plusMinutes(INITIAL_WAIT));

        NumberOfChanges haleKnutsford = routeToRouteCosts.getNumberOfChanges(hale, knutsford, Collections.singleton(Train), when, timeRange);
        assertEquals(0, haleKnutsford.getMin());

        NumberOfChanges knutsfordToHale = routeToRouteCosts.getNumberOfChanges(knutsford, hale, Collections.singleton(Train), when, timeRange);
        assertEquals(0, knutsfordToHale.getMin());

        validateAtLeastOneJourney(Hale, Knutsford);
        validateAtLeastOneJourney(Knutsford, Hale);
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
                Duration.ofMinutes(30), 1, Collections.emptySet());
        Set<Journey> results = testFacade.calculateRouteAsSet(Hale.getId(), Knutsford.getId(), journeyRequest);
        Assertions.assertTrue(results.size()>0);
    }

    @Test
    void produceDiagramOfGraphSubset() throws IOException {
        DiagramCreator creator = componentContainer.get(DiagramCreator.class);
        final Station station = stationRepository.getStationById(Hale.getId());
        assertNotNull(station);
        creator.create(Path.of(format("%s_trains.dot", "around_hale")), station, 100, false);
    }

    private void validateAtLeastOneJourney(RailStationIds start, RailStationIds dest) {
        JourneyRequest journeyRequest = new JourneyRequest(when, tramTime, false, 0,
                Duration.ofMinutes(30), 1, Collections.emptySet());

        Set<Journey> results = testFacade.calculateRouteAsSet(start.getId(), dest.getId(), journeyRequest);
        assertFalse(results.isEmpty(), "No results from " + start + " to " + dest);
    }

    private static class SubgraphConfig extends IntegrationRailTestConfig {
        public SubgraphConfig() {
            super("subgraph_hale_trains_tramchester.db");
        }

        @Override
        public int getMaxInitialWait() {
            return INITIAL_WAIT;
        }
    }
}

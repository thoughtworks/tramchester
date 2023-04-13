package com.tramchester.integration.graph.rail;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.DiagramCreator;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.NumberOfChanges;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.filters.ConfigurableGraphFilter;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.graph.search.routes.RouteToRouteCosts;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.integration.testSupport.rail.TestRailConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.TrainTest;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

import static com.tramchester.domain.reference.TransportMode.Train;
import static com.tramchester.integration.testSupport.rail.RailStationIds.Hale;
import static com.tramchester.integration.testSupport.rail.RailStationIds.Knutsford;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;

@TrainTest
class SubGraphAroundKnutsfordRailTest {
    //public static final int INITIAL_WAIT = 60;
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

        TimeRange timeRange = TimeRange.of(tramTime, tramTime.plusMinutes(TestRailConfig.INITIAL_WAIT_MINS));

        EnumSet<TransportMode> transportModes = EnumSet.of(Train);

        NumberOfChanges haleKnutsford = routeToRouteCosts.getNumberOfChanges(hale, knutsford, transportModes, when, timeRange);
        assertEquals(0, haleKnutsford.getMin(), "expected no changes");

        NumberOfChanges knutsfordToHale = routeToRouteCosts.getNumberOfChanges(knutsford, hale, transportModes, when, timeRange);
        assertEquals(0, knutsfordToHale.getMin(), "expected no changes");

        Duration maxJourneyDuration = Duration.ofMinutes(60);
        validateAtLeastOneJourney(Hale, Knutsford, maxJourneyDuration);
        validateAtLeastOneJourney(Knutsford, Hale, maxJourneyDuration);
    }

    @SuppressWarnings("JUnitTestMethodWithNoAssertions")
    @Test
    void shouldHaveJourneysBetweenAllStations() {
        Duration maxJourneyDuration = Duration.ofMinutes(60);

        for (RailStationIds start: stations) {
            for (RailStationIds destination: stations) {
                if (!start.equals(destination)) {
                    validateAtLeastOneJourney(start, destination, maxJourneyDuration);
                }
            }
        }
    }

    @Test
    void shouldHaveNoChangesBetweenAllStations() {
        TimeRange timeRange = TimeRange.of(tramTime, tramTime.plusMinutes(TestRailConfig.INITIAL_WAIT_MINS));
        EnumSet<TransportMode> transportModes = EnumSet.of(Train);

        for (RailStationIds startId: stations) {
            for (RailStationIds destinationId: stations) {
                if (!startId.equals(destinationId)) {
                    NumberOfChanges numberOfChanges = routeToRouteCosts.getNumberOfChanges(startId.from(stationRepository),
                            destinationId.from(stationRepository),
                            transportModes, when, timeRange);
                    assertEquals(0, numberOfChanges.getMin());
                }
            }
        }
    }

    @Test
    void shouldHaveSimpleJourney() {
        JourneyRequest journeyRequest = new JourneyRequest(when, tramTime, false, 0,
                Duration.ofMinutes(30), 1, EnumSet.noneOf(TransportMode.class));
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

    private void validateAtLeastOneJourney(RailStationIds start, RailStationIds dest, Duration maxJourneyDuration) {
        EnumSet<TransportMode> allModes = EnumSet.noneOf(TransportMode.class);
        JourneyRequest journeyRequest = new JourneyRequest(when, tramTime, false, 0,
                maxJourneyDuration, 1, allModes);

        Set<Journey> results = testFacade.calculateRouteAsSet(start.getId(), dest.getId(), journeyRequest);
        assertFalse(results.isEmpty(), "No results from " + start + " to " + dest + " for " + journeyRequest);
    }

    private static class SubgraphConfig extends IntegrationRailTestConfig {
        public SubgraphConfig() {
            super("subgraph_hale_trains_tramchester.db");
        }

//        @Override
//        public int getMaxInitialWait() {
//            return INITIAL_WAIT;
//        }
    }
}

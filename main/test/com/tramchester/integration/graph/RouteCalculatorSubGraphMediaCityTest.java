package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.DiagramCreator;
import com.tramchester.testSupport.TestConfig;
import com.tramchester.domain.Location;
import com.tramchester.domain.Journey;
import com.tramchester.domain.Station;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphFilter;
import com.tramchester.graph.RouteCalculator;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.testSupport.RouteCodesForTesting;
import com.tramchester.testSupport.Stations;
import org.junit.*;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.junit.Assert.assertTrue;

public class RouteCalculatorSubGraphMediaCityTest {
    private static Dependencies dependencies;
    private static GraphDatabaseService database;

    private RouteCalculator calculator;
    private LocalDate nextTuesday = TestConfig.nextTuesday(0);
    private GraphDatabaseService graphService;

    private static List<Station> stations = Arrays.asList(
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
    private Transaction tx;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws IOException {
        GraphFilter graphFilter = new GraphFilter();
        graphFilter.addRoute(RouteCodesForTesting.ASH_TO_ECCLES);
        graphFilter.addRoute(RouteCodesForTesting.ROCH_TO_DIDS);
        graphFilter.addRoute(RouteCodesForTesting.ECCLES_TO_ASH);
        graphFilter.addRoute(RouteCodesForTesting.DIDS_TO_ROCH);

        stations.forEach(graphFilter::addStation);

        dependencies = new Dependencies(graphFilter);
        dependencies.initialise(new SubgraphConfig());

        database = dependencies.get(GraphDatabaseService.class);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Before
    public void beforeEachTestRuns() {
        graphService = dependencies.get(GraphDatabaseService.class);
        calculator = dependencies.get(RouteCalculator.class);
        tx = database.beginTx();
    }

    @After
    public void afterEachTestRuns() {
        tx.close();
    }

    @Test
    public void shouldHaveMediaCityToExchangeSquare() {
        validateAtLeastOneJourney(Stations.MediaCityUK, Stations.Cornbrook, TramTime.of(9,0), TestConfig.nextSaturday());
        validateAtLeastOneJourney(Stations.MediaCityUK, Stations.ExchangeSquare, TramTime.of(9,0), TestConfig.nextSaturday());
        validateAtLeastOneJourney(Stations.MediaCityUK, Stations.ExchangeSquare, TramTime.of(9,0), TestConfig.nextSunday());
    }

    @Test
    public void shouldHaveJourneyFromEveryStationToEveryOther() {
        List<String> failures = new LinkedList<>();

        for (Station start: stations) {
            for (Station destination: stations) {
                if (!start.equals(destination)) {
                    for (int i = 0; i < 7; i++) {
                        LocalDate day = nextTuesday.plusDays(i);
                        Set<Journey> journeys = calculator.calculateRoute(start.getId(), destination, TramTime.of(9,0),
                                new TramServiceDate(day)).collect(Collectors.toSet());
                        if (journeys.isEmpty()) {
                            failures.add(day.getDayOfWeek() +": "+start+"->"+destination);
                        }
                    }
                }
            }
        }
        assertTrue(failures.isEmpty());
    }

    @Test
    public void reproduceMediaCityIssue() {
        validateAtLeastOneJourney(Stations.ExchangeSquare, Stations.MediaCityUK, TramTime.of(12,0), nextTuesday);
    }

    @Test
    public void reproduceMediaCityIssueSaturdays() {
        validateAtLeastOneJourney(Stations.ExchangeSquare, Stations.MediaCityUK, TramTime.of(9,0), TestConfig.nextSaturday());
    }

    @Test
    public void shouldHaveSimpleJourney() {
        Set<Journey> results = calculator.calculateRoute(Stations.Pomona.getId(), Stations.MediaCityUK,
                TramTime.of(12, 0), new TramServiceDate(nextTuesday)).collect(Collectors.toSet());
        assertTrue(results.size()>0);
    }

    @Ignore
    @Test
    public void produceDiagramOfGraphSubset() throws IOException {
        DiagramCreator creator = new DiagramCreator(graphService, 5);
        List<String> toDraw = new ArrayList<>();
        toDraw.add(Stations.MediaCityUK.getId());

        creator.create(format("%s_trams.dot", "subgraph_mediacity"), toDraw);
    }

    private static class SubgraphConfig extends IntegrationTramTestConfig {
        @Override
        public String getGraphName() {
            return "int_test_sub_mediacity_tramchester.db";
        }

        @Override
        public boolean getRebuildGraph() {
            return true;
        }
    }

    private void validateAtLeastOneJourney(Location start, Station dest, TramTime time, LocalDate date) {
        RouteCalculatorTest.validateAtLeastOneJourney(calculator, start.getId(), dest, time, date);
    }
}

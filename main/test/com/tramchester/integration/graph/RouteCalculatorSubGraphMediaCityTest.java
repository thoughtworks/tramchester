package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.DiagramCreator;
import com.tramchester.TestConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Location;
import com.tramchester.domain.RawJourney;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.TramTime;
import com.tramchester.graph.GraphFilter;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.graph.RouteCalculator;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.integration.RouteCodesForTesting;
import com.tramchester.integration.Stations;
import org.junit.*;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RouteCalculatorSubGraphMediaCityTest {
    private static Dependencies dependencies;
    private static GraphDatabaseService database;

    private RouteCalculator calculator;
    private LocalDate nextTuesday = TestConfig.nextTuesday(0);
    private GraphDatabaseService graphService;
    private RelationshipFactory relationshipFactory;
    private NodeFactory nodeFactory;
    private TramchesterConfig testConfig = new IntegrationTramTestConfig();

    private static List<String> stations = Arrays.asList(
            Stations.ExchangeSquare.getId(),
            Stations.StPetersSquare.getId(),
            Stations.Deansgate.getId(),
            Stations.Cornbrook.getId(),
            Stations.Pomona.getId(),
            "9400ZZMAEXC", // Exchange Quay
            "9400ZZMASQY", // Salford Quays
            "9400ZZMAANC", // Anchorage
            Stations.HarbourCity.getId(),
            Stations.MediaCityUK.getId(),
            Stations.TraffordBar.getId());
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
        relationshipFactory = dependencies.get(RelationshipFactory.class);
        nodeFactory = dependencies.get(NodeFactory.class);
        calculator = dependencies.get(RouteCalculator.class);
        tx = database.beginTx();
    }

    @After
    public void afterEachTestRuns() {
        tx.close();
    }

    @Test
    public void shouldHaveMediaCityToExchangeSquare() {
        validateAtLeastOneJourney(Stations.MediaCityUK, Stations.Cornbrook, TramTime.of(9,00), TestConfig.nextSaturday());
        validateAtLeastOneJourney(Stations.MediaCityUK, Stations.ExchangeSquare, TramTime.of(9,00), TestConfig.nextSaturday());
        validateAtLeastOneJourney(Stations.MediaCityUK, Stations.ExchangeSquare, TramTime.of(9,00), TestConfig.nextSunday());
    }

    @Test
    public void shouldHaveJourneyFromEveryStationToEveryOther() {
        List<TramTime> times = Collections.singletonList(TramTime.of(9,0));
        List<String> failures = new LinkedList<>();

        for (String start: stations) {
            for (String destination: stations) {
                if (!start.equals(destination)) {
                    for (int i = 0; i < 7; i++) {
                        LocalDate day = nextTuesday.plusDays(i);
                        Set<RawJourney> journeys = calculator.calculateRoute(start, destination, times,
                                new TramServiceDate(day), RouteCalculator.MAX_NUM_GRAPH_PATHS).collect(Collectors.toSet());
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
        List<TramTime> minutes = Collections.singletonList(TramTime.of(12, 0));
        Set<RawJourney> results = calculator.calculateRoute(Stations.Pomona.getId(), Stations.MediaCityUK.getId(),
                minutes, new TramServiceDate(nextTuesday), RouteCalculator.MAX_NUM_GRAPH_PATHS).collect(Collectors.toSet());
        assertTrue(results.size()>0);
    }

    @Ignore
    @Test
    public void produceDiagramOfGraphSubset() throws IOException {
        DiagramCreator creator = new DiagramCreator(nodeFactory, relationshipFactory, graphService, 5);
        List<String> toDraw = new ArrayList<>();
//        stations.add(Stations.Cornbrook.getId());
        toDraw.add(Stations.MediaCityUK.getId());
//        stations.add(Stations.StPetersSquare.getId());

        creator.create(format("%s_trams.dot", "subgraph_mediacity"), toDraw);
    }

    private static class SubgraphConfig extends IntegrationTramTestConfig {
        @Override
        public String getGraphName() {
            return "int_test_sub_tramchester.db";
        }

        @Override
        public boolean getRebuildGraph() {
            return true;
        }
    }

    private void validateAtLeastOneJourney(Location start, Location dest, TramTime time, LocalDate date) {
        RouteCalculatorTest.validateAtLeastOneJourney(calculator, start.getId(), dest.getId(), time, date, testConfig.getEdgePerTrip());
    }
}

package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.DiagramCreator;
import com.tramchester.TestConfig;
import com.tramchester.domain.Location;
import com.tramchester.domain.Journey;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.TramTime;
import com.tramchester.graph.GraphFilter;
import com.tramchester.graph.RouteCalculator;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.integration.Stations;
import org.junit.*;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.junit.Assert.assertTrue;

public class RouteCalculatorSubGraphTest {
    private static Dependencies dependencies;
    private static GraphDatabaseService database;

    private RouteCalculator calculator;
    private LocalDate nextTuesday = TestConfig.nextTuesday(0);
    private GraphDatabaseService graphService;
    private static List<Location> stations = Arrays.asList(Stations.Cornbrook, Stations.StPetersSquare, Stations.Deansgate, Stations.Pomona);
    private Transaction tx;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws IOException {
        GraphFilter graphFilter = new GraphFilter();
//        graphFilter.addRoute(RouteCodesForTesting.ALTY_TO_BURY);

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
    public void onceAfterEveryTest() {
        tx.close();
    }

    @Test
    public void reproduceIssueEdgePerTrip() {

        validateAtLeastOneJourney(Stations.StPetersSquare, Stations.Deansgate, TramTime.of(19,51), nextTuesday);
        validateAtLeastOneJourney(Stations.Cornbrook, Stations.Pomona, TramTime.of(19,51).plusMinutes(6), nextTuesday);

        validateAtLeastOneJourney(Stations.Deansgate, Stations.Cornbrook, TramTime.of(19,51).plusMinutes(3), nextTuesday);
        validateAtLeastOneJourney(Stations.Deansgate, Stations.Pomona, TramTime.of(19,51).plusMinutes(3), nextTuesday);

        validateAtLeastOneJourney(Stations.StPetersSquare, Stations.Pomona, TramTime.of(19,51), nextTuesday);
        validateAtLeastOneJourney(Stations.StPetersSquare, Stations.Pomona, TramTime.of(19,56), nextTuesday);
    }

    @Test
    public void shouldHandleCrossingMidnightDirect() {
        validateAtLeastOneJourney(Stations.Cornbrook, Stations.StPetersSquare, TramTime.of(23,55), nextTuesday);
    }

    @Test
    public void shouldHaveJourneysBetweenAllStations() {
        TramTime time = TramTime.of(9, 0);
        for (Location start: stations) {
            for (Location destination: stations) {
                if (!start.equals(destination)) {
                    for (int i = 0; i < 7; i++) {
                        LocalDate day = nextTuesday.plusDays(i);
                        validateAtLeastOneJourney(start, destination, time, day);
                    }
                }
            }
        }
    }

    @Test
    public void shouldHaveSimpleOneStopJourney() {
        Set<Journey> results = calculator.calculateRoute(Stations.Cornbrook.getId(), Stations.Pomona.getId(),
                TramTime.of(8, 0), new TramServiceDate(nextTuesday)).collect(Collectors.toSet());;
        assertTrue(results.size()>0);
    }

    @Test
    public void shouldHaveSimpleOneStopJourneyBetweenInterchanges() {
        Set<Journey> results = calculator.calculateRoute(Stations.StPetersSquare.getId(), Stations.Deansgate.getId(),
                TramTime.of(8, 0), new TramServiceDate(nextTuesday)).collect(Collectors.toSet());
        assertTrue(results.size()>0);
    }

    @Test
    public void shouldHaveSimpleJourney() {
        Set<Journey> results = calculator.calculateRoute(Stations.StPetersSquare.getId(), Stations.Cornbrook.getId(),
                TramTime.of(8, 0), new TramServiceDate(nextTuesday)).collect(Collectors.toSet());
        assertTrue(results.size()>0);
    }

    @Test
    @Ignore
    public void produceDiagramOfGraphSubset() throws IOException {
        DiagramCreator creator = new DiagramCreator(graphService, 7);
        creator.create(format("%s_trams.dot", "subgraph"), Collections.singletonList(Stations.Cornbrook.getId()));
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
        RouteCalculatorTest.validateAtLeastOneJourney(calculator, start.getId(), dest.getId(), time, date);
    }
}

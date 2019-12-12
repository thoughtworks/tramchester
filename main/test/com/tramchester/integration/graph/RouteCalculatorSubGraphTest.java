package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.DiagramCreator;
import com.tramchester.TestConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Location;
import com.tramchester.domain.RawJourney;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.graph.GraphFilter;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.graph.RouteCalculator;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.integration.Stations;
import org.junit.*;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RouteCalculatorSubGraphTest {
    private static Dependencies dependencies;

    private RouteCalculator calculator;
    private LocalDate nextTuesday = TestConfig.nextTuesday(0);
    private GraphDatabaseService graphService;
    private RelationshipFactory relationshipFactory;
    private NodeFactory nodeFactory;
    private TramchesterConfig testConfig = new IntegrationTramTestConfig();
    private static List<Location> stations = Arrays.asList(Stations.Cornbrook, Stations.StPetersSquare, Stations.Deansgate, Stations.Pomona);

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws IOException {
        GraphFilter graphFilter = new GraphFilter();
//        graphFilter.addRoute(RouteCodesForTesting.ALTY_TO_BURY);

        stations.forEach(graphFilter::addStation);

        dependencies = new Dependencies(graphFilter);
        dependencies.initialise(new SubgraphConfig());
    }

    @Before
    public void beforeEachTestRuns() {
        graphService = dependencies.get(GraphDatabaseService.class);
        relationshipFactory = dependencies.get(RelationshipFactory.class);
        nodeFactory = dependencies.get(NodeFactory.class);
        calculator = dependencies.get(RouteCalculator.class);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Test
    public void reproduceIssueEdgePerTrip() {

        validateAtLeastOneJourney(Stations.StPetersSquare, Stations.Deansgate, LocalTime.of(19,51), nextTuesday);
        validateAtLeastOneJourney(Stations.Cornbrook, Stations.Pomona, LocalTime.of(19,51).plusMinutes(6), nextTuesday);

        validateAtLeastOneJourney(Stations.Deansgate, Stations.Cornbrook, LocalTime.of(19,51).plusMinutes(3), nextTuesday);
        validateAtLeastOneJourney(Stations.Deansgate, Stations.Pomona, LocalTime.of(19,51).plusMinutes(3), nextTuesday);

        validateAtLeastOneJourney(Stations.StPetersSquare, Stations.Pomona, LocalTime.of(19,51), nextTuesday);
        validateAtLeastOneJourney(Stations.StPetersSquare, Stations.Pomona, LocalTime.of(19,56), nextTuesday);
    }

    @Test
    public void shouldHaveJourneysBetweenAllStations() {
        LocalTime time = LocalTime.of(9, 00);
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
        List<LocalTime> minutes = Collections.singletonList(LocalTime.of(8, 0));
        Set<RawJourney> results = calculator.calculateRoute(Stations.Cornbrook.getId(), Stations.Pomona.getId(),
                minutes, new TramServiceDate(nextTuesday), RouteCalculator.MAX_NUM_GRAPH_PATHS);
        assertTrue(results.size()>0);
    }

    @Test
    public void shouldHaveSimpleOneStopJourneyBetweenInterchanges() {
        List<LocalTime> minutes = Collections.singletonList(LocalTime.of(8, 0));
        Set<RawJourney> results = calculator.calculateRoute(Stations.StPetersSquare.getId(), Stations.Deansgate.getId(),
                minutes, new TramServiceDate(nextTuesday), RouteCalculator.MAX_NUM_GRAPH_PATHS);
        assertTrue(results.size()>0);
    }

    @Test
    public void shouldHaveSimpleJourney() {
        List<LocalTime> minutes = Collections.singletonList(LocalTime.of(8, 0));
        Set<RawJourney> results = calculator.calculateRoute(Stations.StPetersSquare.getId(), Stations.Cornbrook.getId(),
                minutes, new TramServiceDate(nextTuesday), RouteCalculator.MAX_NUM_GRAPH_PATHS);
        assertTrue(results.size()>0);
    }

    @Test
    @Ignore
    public void produceDiagramOfGraphSubset() throws IOException {
        DiagramCreator creator = new DiagramCreator(nodeFactory, relationshipFactory, graphService, 7);
        creator.create(format("%s_trams.dot", "subgraph"), Collections.singletonList(Stations.Cornbrook.getId()));
    }

    private static class SubgraphConfig extends IntegrationTramTestConfig {
        @Override
        public String getGraphName() {
            return "int_test__sub_tramchester.db";
        }

        @Override
        public boolean getRebuildGraph() {
            return true;
        }
    }

    private void validateAtLeastOneJourney(Location start, Location dest, LocalTime time, LocalDate date) {
        RouteCalculatorTest.validateAtLeastOneJourney(calculator, start.getId(), dest.getId(), time, date, testConfig.getEdgePerTrip());
    }
}

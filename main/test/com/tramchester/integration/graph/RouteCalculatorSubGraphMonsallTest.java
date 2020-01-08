package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.TestConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Location;
import com.tramchester.domain.RawJourney;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.TramTime;
import com.tramchester.graph.GraphFilter;
import com.tramchester.graph.RouteCalculator;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.integration.RouteCodesForTesting;
import com.tramchester.integration.Stations;
import org.junit.*;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.junit.Assert.*;

public class RouteCalculatorSubGraphMonsallTest {
    private static Dependencies dependencies;
    private static GraphDatabaseService database;

    private RouteCalculator calculator;
    private LocalDate nextTuesday = TestConfig.nextTuesday(0);
    private Transaction tx;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws IOException {
        GraphFilter graphFilter = new GraphFilter();
        graphFilter.addRoute(RouteCodesForTesting.DIDS_TO_ROCH);

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
        calculator = dependencies.get(RouteCalculator.class);
        tx = database.beginTx();
    }

    @After
    public void onceAfterEveryTest() {
        tx.close();
    }

    @Test
    public void reproduceIssueWithTooManyStages() {
        validateOneStageJourney(Stations.Monsall, Stations.RochdaleRail, TramTime.of(8,0), nextTuesday);
    }

    @Test
    public void shouldHaveSimpleOneStopJourney() {
        validateOneStageJourney(Stations.RochdaleRail, Stations.Rochdale, TramTime.of(8,0), nextTuesday);
    }

    private static class SubgraphConfig extends IntegrationTramTestConfig {
        @Override
        public String getGraphName() {
            return "int_test_sub_monsall_tramchester.db";
        }

        @Override
        public boolean getRebuildGraph() {
            return true;
        }
    }

    private void validateOneStageJourney(Location start, Location dest, TramTime time, LocalDate date) {
        Set<RawJourney> journeys = calculator.calculateRoute(start.getId(), dest.getId(), Collections.singletonList(time),
                new TramServiceDate(date), RouteCalculator.MAX_NUM_GRAPH_PATHS).
                collect(Collectors.toSet());

        assertFalse(journeys.isEmpty());
        journeys.forEach(journey -> assertEquals(1, journey.getStages().size()));
    }
}

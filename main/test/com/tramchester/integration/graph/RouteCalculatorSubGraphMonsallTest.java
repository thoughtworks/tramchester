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
    public void reproduceIssueWithTooManyStages() {
        tx.close();
    }

    @Test
    public void shouldReproIssueWithNotFindingDirectRouting() {
//        validateOneStageJourney(Stations.Monsall, Stations.ShawAndCrompton, TramTime.of(8,0), nextTuesday);
//
        String derkerStationId = "9400ZZMADER";
//        validateOneStageJourney(derkerStationId, Stations.RochdaleRail.getId(), TramTime.of(8,0), nextTuesday);
//        validateOneStageJourney(Stations.Monsall.getId(), derkerStationId, TramTime.of(8,0), nextTuesday);

//        validateOneStageJourney(derkerStationId, Stations.RochdaleRail.getId(), TramTime.of(8,28), nextTuesday);

        validateOneStageJourney(Stations.Monsall, Stations.RochdaleRail, TramTime.of(8,5), nextTuesday);

//        for (int minute = 0; minute <60; minute=minute+4) {
//            validateOneStageJourney(Stations.Monsall, Stations.RochdaleRail, TramTime.of(8,minute), nextTuesday);
//        }
    }

    @Test
    public void shouldHaveEndToEnd() {
        validateOneStageJourney(Stations.EastDidsbury, Stations.Rochdale, TramTime.of(8,0), nextTuesday);
    }

    @Test
    public void shouldHaveJourneysTerminationPointsToEndOfLine() {
        // many trams only run as far as Shaw
        validateOneStageJourney(Stations.ShawAndCrompton, Stations.Rochdale, TramTime.of(8,0), nextTuesday);
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

        String startId = start.getId();
        String destId = dest.getId();

        validateOneStageJourney( startId, destId,time, date);
    }

    private void validateOneStageJourney(String startId, String destId, TramTime time, LocalDate date) {
        Set<RawJourney> journeys = calculator.calculateRoute(startId, destId, Collections.singletonList(time),
                new TramServiceDate(date), RouteCalculator.MAX_NUM_GRAPH_PATHS).
                collect(Collectors.toSet());

        assertFalse(format("No Journeys from %s to %s found at %s on %s", startId, destId, time.toString(), date), journeys.isEmpty());
        journeys.forEach(journey -> assertEquals(1, journey.getStages().size()));
    }
}

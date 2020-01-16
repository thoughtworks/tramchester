package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.TestConfig;
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
//        String derkerStationId = "9400ZZMADER";

        // change at shaw
        validateNumberOfStages(Stations.Monsall, Stations.RochdaleRail, TramTime.of(8,5),
                nextTuesday, 2);

        // direct
        validateNumberOfStages(Stations.Monsall, Stations.RochdaleRail, TramTime.of(8,10),
                nextTuesday, 1);
    }

    @Test
    public void shouldHaveEndToEnd() {
        validateNumberOfStages(Stations.EastDidsbury, Stations.Rochdale, TramTime.of(8,0), nextTuesday, 1);
    }

    @Test
    public void shouldHaveJourneysTerminationPointsToEndOfLine() {
        // many trams only run as far as Shaw
        validateNumberOfStages(Stations.ShawAndCrompton, Stations.Rochdale, TramTime.of(8,0), nextTuesday, 1);
    }

    @Test
    public void shouldHaveSimpleOneStopJourney() {
        validateNumberOfStages(Stations.RochdaleRail, Stations.Rochdale, TramTime.of(8,0), nextTuesday, 1);
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

    private void validateNumberOfStages(Location start, Location dest, TramTime time, LocalDate date, int numStages) {

        String startId = start.getId();
        String destId = dest.getId();

        validateNumberOfStages( startId, destId,time, date, numStages);
    }

    private void validateNumberOfStages(String startId, String destId, TramTime time, LocalDate date, int numStages) {
        Set<RawJourney> journeys = calculator.calculateRoute(startId, destId, Collections.singletonList(time),
                new TramServiceDate(date)).
                collect(Collectors.toSet());

        assertFalse(format("No Journeys from %s to %s found at %s on %s", startId, destId, time.toString(), date), journeys.isEmpty());
        journeys.forEach(journey -> assertEquals(numStages, journey.getStages().size()));
    }
}

package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.domain.Journey;
import com.tramchester.domain.Location;
import com.tramchester.domain.Station;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.ActiveGraphFilter;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.RouteCalculator;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.testSupport.RouteCodesForTesting;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestEnv;
import org.junit.*;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class RouteCalculatorSubGraphMonsallTest {
    private static Dependencies dependencies;
    private static GraphDatabase database;

    private RouteCalculator calculator;
    private LocalDate nextTuesday = TestEnv.nextTuesday(0);
    private Transaction tx;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws IOException {
        ActiveGraphFilter graphFilter = new ActiveGraphFilter();
        graphFilter.addRoute(RouteCodesForTesting.DIDS_TO_ROCH);

        dependencies = new Dependencies(graphFilter);
        dependencies.initialise(new SubgraphConfig());

        database = dependencies.get(GraphDatabase.class);
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

        // Can be direct or with a change depending on the timetable data
        validateNumberOfStages(Stations.Monsall, Stations.RochdaleRail, TramTime.of(8,5),
                nextTuesday, 1);

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

        public SubgraphConfig() {
            super("sub_monsall_tramchester.db");
        }

        @Override
        public boolean getRebuildGraph() {
            return true;
        }
    }

    private void validateNumberOfStages(Location start, Station dest, TramTime time, LocalDate date, int numStages) {
        String startId = start.getId();
        validateNumberOfStages(startId, dest, time, date, numStages);
    }

    private void validateNumberOfStages(String startId, Station destination, TramTime time, LocalDate date, int numStages) {
        Set<Journey> journeys = calculator.calculateRoute(startId, destination, time,
                new TramServiceDate(date)).
                collect(Collectors.toSet());

        assertFalse(format("No Journeys from %s to %s found at %s on %s", startId, destination, time.toString(), date),
                journeys.isEmpty());
        journeys.forEach(journey -> assertEquals(numStages, journey.getStages().size()));
    }
}

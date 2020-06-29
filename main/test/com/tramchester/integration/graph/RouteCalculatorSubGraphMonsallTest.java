package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.domain.Journey;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.graphbuild.ActiveGraphFilter;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.testSupport.RoutesForTesting;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

class RouteCalculatorSubGraphMonsallTest {
    private static Dependencies dependencies;
    private static GraphDatabase database;

    private RouteCalculator calculator;
    private final LocalDate when = TestEnv.testDay();
    private Transaction txn;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {
        ActiveGraphFilter graphFilter = new ActiveGraphFilter();
        graphFilter.addRoute(RoutesForTesting.DIDS_TO_ROCH);

        dependencies = new Dependencies(graphFilter);
        dependencies.initialise(new SubgraphConfig());

        database = dependencies.get(GraphDatabase.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        calculator = dependencies.get(RouteCalculator.class);
        txn = database.beginTx();
    }

    @AfterEach
    void reproduceIssueWithTooManyStages() {
        txn.close();
    }

    @Test
    void shouldReproIssueWithNotFindingDirectRouting() {

        // Can be direct or with a change depending on the timetable data
        validateNumberOfStages(Stations.Monsall, Stations.RochdaleRail, TramTime.of(8,5),
                when, 1);

        // direct
        validateNumberOfStages(Stations.Monsall, Stations.RochdaleRail, TramTime.of(8,10),
                when, 1);
    }

    @Test
    void shouldHaveEndToEnd() {
        validateNumberOfStages(Stations.EastDidsbury, Stations.Rochdale, TramTime.of(8,0), when, 1);
    }

    @Test
    void shouldHaveJourneysTerminationPointsToEndOfLine() {
        // many trams only run as far as Shaw
        validateNumberOfStages(Stations.ShawAndCrompton, Stations.Rochdale, TramTime.of(8,0), when, 1);
    }

    @Test
    void shouldHaveSimpleOneStopJourney() {
        validateNumberOfStages(Stations.RochdaleRail, Stations.Rochdale, TramTime.of(8,0), when, 1);
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

    private void validateNumberOfStages(Station start, Station destination, TramTime time, LocalDate date, int numStages) {
        Set<Journey> journeys = calculator.calculateRoute(txn, start, destination, new JourneyRequest(new TramServiceDate(date), time,
                false)).
                collect(Collectors.toSet());

        Assertions.assertFalse(
                journeys.isEmpty(), format("No Journeys from %s to %s found at %s on %s", start, destination, time.toString(), date));
        journeys.forEach(journey -> Assertions.assertEquals(numStages, journey.getStages().size()));
    }
}

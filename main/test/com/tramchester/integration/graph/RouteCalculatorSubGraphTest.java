package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.DiagramCreator;
import com.tramchester.domain.Journey;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.graphbuild.ActiveGraphFilter;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

class RouteCalculatorSubGraphTest {
    private static Dependencies dependencies;
    private static GraphDatabase database;

    private RouteCalculator calculator;
    private final LocalDate when = TestEnv.testDay();
    private static final List<Station> stations = Arrays.asList(
            Stations.Cornbrook,
            Stations.StPetersSquare,
            Stations.Deansgate,
            Stations.Pomona);
    private Transaction txn;
    private TramTime tramTime;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {
        ActiveGraphFilter graphFilter = new ActiveGraphFilter();
//        graphFilter.addRoute(RouteCodesForTesting.ALTY_TO_BURY);

        stations.forEach(graphFilter::addStation);

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
        tramTime = TramTime.of(8, 0);
    }

    @AfterEach
    void onceAfterEveryTest() {
        txn.close();
    }

    @SuppressWarnings("JUnitTestMethodWithNoAssertions")
    @Test
    void reproduceIssueEdgePerTrip() {

        validateAtLeastOneJourney(Stations.StPetersSquare, Stations.Deansgate, TramTime.of(19,51), when);
        validateAtLeastOneJourney(Stations.Cornbrook, Stations.Pomona, TramTime.of(19,51).plusMinutes(6), when);

        validateAtLeastOneJourney(Stations.Deansgate, Stations.Cornbrook, TramTime.of(19,51).plusMinutes(3), when);
        validateAtLeastOneJourney(Stations.Deansgate, Stations.Pomona, TramTime.of(19,51).plusMinutes(3), when);

        validateAtLeastOneJourney(Stations.StPetersSquare, Stations.Pomona, TramTime.of(19,51), when);
        validateAtLeastOneJourney(Stations.StPetersSquare, Stations.Pomona, TramTime.of(19,56), when);
    }

    @SuppressWarnings("JUnitTestMethodWithNoAssertions")
    @Disabled("Temporary: trams finish at 2300")
    @Test
    void shouldHandleCrossingMidnightDirect() {
        validateAtLeastOneJourney(Stations.Cornbrook, Stations.StPetersSquare, TramTime.of(23,55), when);
    }

    @SuppressWarnings("JUnitTestMethodWithNoAssertions")
    @Test
    void shouldHaveJourneysBetweenAllStations() {
        for (Station start: stations) {
            for (Station destination: stations) {
                if (!start.equals(destination)) {
                    validateAtLeastOneJourney(start, destination, tramTime, when);
                }
            }
        }
    }

    @Test
    void shouldHaveSimpleOneStopJourney() {
        Set<Journey> results = calculator.calculateRoute(txn, Stations.Cornbrook, Stations.Pomona,
                new JourneyRequest(new TramServiceDate(when), tramTime, false)).collect(Collectors.toSet());
        Assertions.assertTrue(results.size()>0);
    }

    @Test
    void shouldHaveSimpleOneStopJourneyAtWeekend() {
        Set<Journey> results = calculator.calculateRoute(txn, Stations.Cornbrook, Stations.Pomona,
                new JourneyRequest(new TramServiceDate(TestEnv.nextSaturday()), tramTime, false)).collect(Collectors.toSet());
        Assertions.assertTrue(results.size()>0);
    }

    @Test
    void shouldHaveSimpleOneStopJourneyBetweenInterchanges() {
        Set<Journey> results = calculator.calculateRoute(txn, Stations.StPetersSquare, Stations.Deansgate,
                new JourneyRequest(new TramServiceDate(when), tramTime, false)).collect(Collectors.toSet());
        Assertions.assertTrue(results.size()>0);
    }

    @Test
    void shouldHaveSimpleJourney() {
        Set<Journey> results = calculator.calculateRoute(txn, Stations.StPetersSquare, Stations.Cornbrook,
                new JourneyRequest(new TramServiceDate(when), tramTime, false)).collect(Collectors.toSet());
        Assertions.assertTrue(results.size()>0);
    }

    @Test
    //@Disabled
    void produceDiagramOfGraphSubset() throws IOException {
        DiagramCreator creator = new DiagramCreator(database, 7);
        creator.create(format("%s_trams.dot", "subgraph"), Collections.singletonList(Stations.Cornbrook.getId()));
    }

    private static class SubgraphConfig extends IntegrationTramTestConfig {
        public SubgraphConfig() {
            super("subgraph_tramchester.db");
        }


        @Override
        public boolean getRebuildGraph() {
            return true;
        }
    }

    private void validateAtLeastOneJourney(Station start, Station dest, TramTime time, LocalDate date) {
        RouteCalculatorTest.validateAtLeastNJourney(calculator, 1, txn, start, dest, time, date, 5);
    }
}

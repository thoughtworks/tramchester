package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.domain.Journey;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.graphbuild.ActiveGraphFilter;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.*;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

class RouteCalculatorSubGraphMonsallTest {
    private static Dependencies dependencies;
    private static GraphDatabase database;
    private static SubgraphConfig config;

    private RouteCalculator calculator;
    private final LocalDate when = TestEnv.testDay();
    private Transaction txn;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        ActiveGraphFilter graphFilter = new ActiveGraphFilter();
        graphFilter.addRoute(RoutesForTesting.DIDS_TO_ROCH);

        dependencies = new Dependencies(graphFilter);
        config = new SubgraphConfig();
        dependencies.initialise(config);

        database = dependencies.get(GraphDatabase.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        calculator = dependencies.get(RouteCalculator.class);
        stationRepository = dependencies.get(StationRepository.class);
        txn = database.beginTx();
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldReproIssueWithNotFindingDirectRouting() {

        // Can be direct or with a change depending on the timetable data
        validateNumberOfStages(TramStations.Monsall, TramStations.RochdaleRail, TramTime.of(8,5),
                when, 1);

        // direct
        validateNumberOfStages(TramStations.Monsall, TramStations.RochdaleRail, TramTime.of(8,10),
                when, 1);
    }

    @Test
    void shouldHaveEndToEnd() {
        validateNumberOfStages(TramStations.EastDidsbury, TramStations.Rochdale, TramTime.of(8,0), when, 1);
    }

    @Test
    void shouldHaveJourneysTerminationPointsToEndOfLine() {
        // many trams only run as far as Shaw
        validateNumberOfStages(TramStations.ShawAndCrompton, TramStations.Rochdale, TramTime.of(8,0), when, 1);
    }

    @Test
    void shouldHaveSimpleOneStopJourney() {
        validateNumberOfStages(TramStations.RochdaleRail, TramStations.Rochdale, TramTime.of(8,0), when, 1);
    }

    private static class SubgraphConfig extends IntegrationTramTestConfig {

        public SubgraphConfig() {
            super("sub_monsall_tramchester.db");
        }

    }

    private void validateNumberOfStages(TramStations start, TramStations destination, TramTime time, LocalDate date, int numStages) {


        Set<Journey> journeys = calculator.calculateRoute(txn, TestStation.real(stationRepository, start),
                TestStation.real(stationRepository, destination), new JourneyRequest(new TramServiceDate(date), time,
                false, 3, config.getMaxJourneyDuration())).
                collect(Collectors.toSet());

        Assertions.assertFalse(
                journeys.isEmpty(), format("No Journeys from %s to %s found at %s on %s", start, destination, time.toString(), date));
        journeys.forEach(journey -> Assertions.assertEquals(numStages, journey.getStages().size()));
    }
}

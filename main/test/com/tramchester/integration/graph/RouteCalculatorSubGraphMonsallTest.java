package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Journey;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.testSupport.ActiveGraphFilter;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.graph.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Set;

import static java.lang.String.format;

class RouteCalculatorSubGraphMonsallTest {
    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static SubgraphConfig config;

    private RouteCalculatorTestFacade calculator;
    private final LocalDate when = TestEnv.testDay();
    private Transaction txn;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {
        config = new SubgraphConfig();
        TestEnv.deleteDBIfPresent(config);

        ActiveGraphFilter graphFilter = new ActiveGraphFilter();
        graphFilter.addTramRoute(KnownTramRoute.EastDidisburyManchesterShawandCromptonRochdale);

        componentContainer = new ComponentsBuilder<>().setGraphFilter(graphFilter).create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        database = componentContainer.get(GraphDatabase.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() throws IOException {
        componentContainer.close();
        TestEnv.deleteDBIfPresent(config);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        StationRepository stationRepository = componentContainer.get(StationRepository.class);
        txn = database.beginTx();
        calculator = new RouteCalculatorTestFacade(componentContainer.get(RouteCalculator.class), stationRepository, txn);

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
        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(date), time,
                false, 3, config.getMaxJourneyDuration());
        Set<Journey> journeys = calculator.calculateRouteAsSet(start, destination, journeyRequest);

        Assertions.assertFalse(journeys.isEmpty(), format("No Journeys from %s to %s found at %s on %s", start, destination, time.toString(), date));
        journeys.forEach(journey -> Assertions.assertEquals(numStages, journey.getStages().size()));
    }
}

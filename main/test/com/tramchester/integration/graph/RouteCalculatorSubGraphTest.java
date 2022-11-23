package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.DiagramCreator;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.filters.ConfigurableGraphFilter;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.resources.LocationJourneyPlanner;
import com.tramchester.testSupport.LocationJourneyPlannerTestFacade;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.tramchester.domain.reference.TransportMode.TramsOnly;
import static com.tramchester.testSupport.reference.KnownLocations.nearStPetersSquare;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RouteCalculatorSubGraphTest {
    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static SubgraphConfig config;

    private RouteCalculatorTestFacade calculator;
    private final TramDate when = TestEnv.testDay();
    private static final List<TramStations> stations = Arrays.asList(
            Cornbrook,
            StPetersSquare,
            Deansgate,
            Pomona);
    private Transaction txn;
    private TramTime tramTime;
    private Duration maxJourneyDuration;
    private Set<TransportMode> modes;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {
        config = new SubgraphConfig();
        //TestEnv.deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder().
                configureGraphFilter(RouteCalculatorSubGraphTest::configureFilter).
                create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        database = componentContainer.get(GraphDatabase.class);
    }

    private static void configureFilter(ConfigurableGraphFilter graphFilter, TransportData transportData) {
        stations.forEach(station -> graphFilter.addStation(station.getId()));
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() throws IOException {
        componentContainer.close();
        //TestEnv.deleteDBIfPresent(config);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        StationRepository stationRepository = componentContainer.get(StationRepository.class);
        txn = database.beginTx();
        calculator = new RouteCalculatorTestFacade(componentContainer.get(RouteCalculator.class), stationRepository, txn);

        tramTime = TramTime.of(8, 0);
        maxJourneyDuration = Duration.ofMinutes(config.getMaxJourneyDuration());

        modes = TramsOnly;
    }

    @AfterEach
    void onceAfterEveryTest() {
        txn.close();
    }

    @Test
    void reproduceIssueEdgePerTrip() {

        validateAtLeastOneJourney(StPetersSquare, TramStations.Deansgate,
                new JourneyRequest(when, tramTime, false, 5, maxJourneyDuration, 1, modes));

        validateAtLeastOneJourney(Cornbrook, Pomona,
                new JourneyRequest(when, TramTime.of(19,51).plusMinutes(6), false, 5,
                        maxJourneyDuration, 1, modes));

        validateAtLeastOneJourney(TramStations.Deansgate, Cornbrook,
                new JourneyRequest(when, TramTime.of(19,51).plusMinutes(3), false, 5,
                        maxJourneyDuration, 1, modes));

        validateAtLeastOneJourney(TramStations.Deansgate, Pomona,
                new JourneyRequest(when, TramTime.of(19,51).plusMinutes(3), false, 5,
                        maxJourneyDuration, 1, modes));

        validateAtLeastOneJourney(StPetersSquare, Pomona, new JourneyRequest(when, tramTime,
                false, 5, maxJourneyDuration, 1, modes));
        validateAtLeastOneJourney(StPetersSquare, Pomona, new JourneyRequest(when, tramTime,
                false, 5, maxJourneyDuration, 1, modes));
    }

    @Test
    void shouldHandleCrossingMidnightDirect() {
        validateAtLeastOneJourney(Cornbrook, StPetersSquare, new JourneyRequest(when, tramTime, false, 5,
                maxJourneyDuration, 1, modes));
    }

    @SuppressWarnings("JUnitTestMethodWithNoAssertions")
    @Test
    void shouldHaveJourneysBetweenAllStations() {
        for (TramStations start: stations) {
            for (TramStations destination: stations) {
                if (!start.equals(destination)) {
                    validateAtLeastOneJourney(start, destination, new JourneyRequest(when, tramTime, false, 5,
                            maxJourneyDuration, 1, modes));
                }
            }
        }
    }

    @Test
    void shouldHaveWalkAtEnd() {

        LocationJourneyPlanner planner = componentContainer.get(LocationJourneyPlanner.class);
        StationRepository stationRepository = componentContainer.get(StationRepository.class);
        LocationJourneyPlannerTestFacade testFacade = new LocationJourneyPlannerTestFacade(planner, stationRepository, txn);

        JourneyRequest journeyRequest = new JourneyRequest(when, tramTime, false, 3,
                maxJourneyDuration,1, modes);
        //journeyRequest.setDiag(true);
        final Station station = stationRepository.getStationById(Pomona.getId());
        Set<Journey> results = testFacade.quickestRouteForLocation(station, nearStPetersSquare, journeyRequest, 4);
        assertFalse(results.isEmpty());
    }

    @Test
    void shouldHaveSimpleOneStopJourney() {
        Set<Journey> results = getJourneys(Cornbrook, Pomona, when, 1);
        Assertions.assertTrue(results.size()>0);
    }

    @Test
    void shouldHaveSimpleOneStopJourneyLateNight() {
        // last tram now earlier
        TramTime time = TramTime.of(23,53);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, 3,
                maxJourneyDuration, 1, modes);
        Set<Journey> results = calculator.calculateRouteAsSet(Cornbrook, Pomona, journeyRequest);
        Assertions.assertTrue(results.size()>0);
    }

    @Test
    void shouldHaveSimpleOneStopJourneyAtWeekend() {
        Set<Journey> results = getJourneys(Cornbrook, Pomona, TestEnv.nextSaturday(), 1);
        Assertions.assertTrue(results.size()>0);
    }

    @Test
    void shouldHaveSimpleOneStopJourneyBetweenInterchanges() {
        Set<Journey> results = getJourneys(StPetersSquare, TramStations.Deansgate, when, 1);
        Assertions.assertTrue(results.size()>0);
    }

    @Test
    void shouldHaveSimpleJourney() {
        Set<Journey> results = getJourneys(StPetersSquare, Cornbrook, when, 1);
        Assertions.assertTrue(results.size()>0);
    }

    @Test
    void produceDiagramOfGraphSubset() throws IOException {
        DiagramCreator creator = componentContainer.get(DiagramCreator.class);
        //DiagramCreator creator = new DiagramCreator(database);
        creator.create(Path.of("subgraph_trams.dot"), Cornbrook.fake(), 100, false);
    }

    private static class SubgraphConfig extends IntegrationTramTestConfig {
        public SubgraphConfig() {
            super("subgraph_tramchester.db", Collections.emptyList());
        }
    }

    @NotNull
    private Set<Journey> getJourneys(TramStations start, TramStations destination, TramDate when, long maxNumberJourneys) {
        JourneyRequest journeyRequest = new JourneyRequest(when, tramTime, false, 3,
                maxJourneyDuration, maxNumberJourneys, modes);
        return calculator.calculateRouteAsSet(start,destination, journeyRequest);
    }

    private void validateAtLeastOneJourney(TramStations start, TramStations dest, JourneyRequest journeyRequest) {
        // TODO Use find any on stream instead
        Set<Journey> results = calculator.calculateRouteAsSet(start, dest, journeyRequest);
        assertFalse(results.isEmpty());
    }
}

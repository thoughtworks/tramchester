package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.domain.Journey;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.IntegrationBusTestConfig;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.testSupport.BusStations.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class BusRouteCalculatorTest {
    // TODO this needs to be > time for whole test fixture, see note below in @After
    private static final int TXN_TIMEOUT = 5*60;

    private static Dependencies dependencies;
    private static GraphDatabase database;
    private static IntegrationBusTestConfig testConfig;

    private RouteCalculator calculator;
    private final LocalDate when = TestEnv.testDay();
    private Transaction txn;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        testConfig = new IntegrationBusTestConfig();
        dependencies.initialise(testConfig);
        database = dependencies.get(GraphDatabase.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        txn = database.beginTx(TXN_TIMEOUT, TimeUnit.SECONDS);
        calculator = dependencies.get(RouteCalculator.class);
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldHaveStockToALtyJourney() {
        TramTime travelTime = TramTime.of(7, 0);
        LocalDate nextMonday = TestEnv.nextMonday();

//        GraphQuery graphQuery = dependencies.get(GraphQuery.class);
//
//        String routeStationId = "1800AMIC001WBT:5A:I:";
//        Node node = graphQuery.getRouteStationNode(txn, routeStationId);
//        Iterable<Relationship> services = node.getRelationships(Direction.OUTGOING, TransportRelationshipTypes.TO_SERVICE);
//        Set<Relationship> list = new HashSet<>();
//        services.forEach(list::add);
//
//        assertEquals(4, list.size());

        Set<Journey> journeys = RouteCalculatorTest.validateAtLeastNJourney(calculator, 3, txn, StockportBusStation,
                AltrinchamInterchange, travelTime, nextMonday, 2, testConfig.getMaxJourneyDuration());
        // 2 changes means 3 stages or less
        journeys.forEach(journey -> assertTrue(journey.getStages().size()<=3, journey.getStages().toString()));

        Set<Journey> journeysMaxChanges = RouteCalculatorTest.validateAtLeastNJourney(calculator, 3, txn, AltrinchamInterchange,
                StockportBusStation, travelTime, nextMonday, 8, testConfig.getMaxJourneyDuration());
        // algo seems to return very large number of changes even when 2 is possible??
        List<Journey> journeys2Stages = journeysMaxChanges.stream().filter(journey -> journey.getStages().size() <= 3).collect(Collectors.toList());
        assertFalse(journeys2Stages.isEmpty());
    }

    @Test
    void shouldFindAltyToKnutfordAtExpectedTime() {
        TramTime travelTime = TramTime.of(9, 55);

        Stream<Journey> journeyStream = calculator.calculateRoute(txn, AltrinchamInterchange, KnutsfordStationStand3,
                new JourneyRequest(new TramServiceDate(when), travelTime, false, 8, testConfig.getMaxJourneyDuration()));
        Set<Journey> journeys = journeyStream.collect(Collectors.toSet());
        journeyStream.close();

        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldNotRevisitSameBusStationAltyToKnutsford() {
        TramTime travelTime = TramTime.of(15, 25);

        Stream<Journey> journeyStream = calculator.calculateRoute(txn, AltrinchamInterchange, KnutsfordStationStand3,
                new JourneyRequest(new TramServiceDate(when), travelTime, false, 8, testConfig.getMaxJourneyDuration()));
        Set<Journey> journeys = journeyStream.collect(Collectors.toSet());
        journeyStream.close();

        journeys.forEach(journey -> {
            List<Location> seen = new ArrayList<>();
            journey.getStages().forEach(stage -> {
                Location actionStation = stage.getActionStation();
                assertFalse(seen.contains(actionStation), "Already seen " + actionStation + " for " + journey.toString());
                seen.add(actionStation);
            });
        });
        // Expected no journeys, when working correctly should not find a jounrey at this time,
        // i.e. we should not loop back around to alty
        assertTrue(journeys.isEmpty());
    }

    @Test
    void shouldHaveShudehillToStockJourney() {
        int maxChanges = 2;
        Set<Journey> journeys = RouteCalculatorTest.validateAtLeastNJourney(calculator, 3, txn,
                ShudehillInterchange, StockportBusStation,
                TramTime.of(8, 0), when, maxChanges, testConfig.getMaxJourneyDuration());
        journeys.forEach(journey -> Assertions.assertTrue(journey.getStages().size()<=(maxChanges+1)));
    }

    @SuppressWarnings("JUnitTestMethodWithNoAssertions")
    @Test
    void shouldHaveSimpleTramJourney() {
        RouteCalculatorTest.validateAtLeastNJourney(calculator, 1, txn, Stations.Altrincham, Stations.Cornbrook,
                TramTime.of(8, 0), when, 5, testConfig.getMaxJourneyDuration());
    }
}

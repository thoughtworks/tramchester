package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.resources.LocationJourneyPlanner;
import com.tramchester.testSupport.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.BusStations.*;
import static org.junit.jupiter.api.Assertions.*;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class PostcodeBusRouteCalculatorTest {
    // TODO this needs to be > time for whole test fixture, see note below in @After
    private static final int TXN_TIMEOUT = 5*60;

    private static Dependencies dependencies;
    private static GraphDatabase database;
    private static TramchesterConfig testConfig;

    private final LocalDate day = TestEnv.testDay();
    private Transaction txn;
    private TramTime time = TramTime.of(9,11);
    private LocationJourneyPlanner planner;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        dependencies = new Dependencies();
        testConfig = new BusWithPostcodesEnabled();
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
        planner = dependencies.get(LocationJourneyPlanner.class);
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldPlanJourneyFromPostcodeToPostcodeViaBus() {
        Set<Journey> journeys = planner.quickestRouteForLocation(txn, Postcodes.CentralBury.getLatLong(),
                Postcodes.NearPiccadily.getLatLong(), createRequest(3)).collect(Collectors.toSet());

        assertFalse(journeys.isEmpty());
        journeys.forEach(journey -> assertEquals(3,journey.getStages().size()));
    }

    @NotNull
    private JourneyRequest createRequest(int maxChanges) {
        return new JourneyRequest(new TramServiceDate(day), time, false, maxChanges, testConfig.getMaxJourneyDuration());
    }

    @Test
    void shouldWalkFromPostcodeToNearbyStation() {

        Set<Journey> journeys = planner.quickestRouteForLocation(txn, Postcodes.CentralBury.getLatLong(),
                BusStations.BuryInterchange, createRequest(3)).collect(Collectors.toSet());

        assertFalse(journeys.isEmpty());

        journeys.forEach(journey -> {
            assertEquals(1, journey.getStages().size());
            assertEquals(TransportMode.Walk, journey.getStages().get(0).getMode());
            assertEquals(BusStations.BuryInterchange, getLastStage(journey).getLastStation());
        });
    }

    private TransportStage getLastStage(Journey journey) {
        return journey.getStages().get(journey.getStages().size()-1);
    }

    @Test
    void shouldWalkFromBusStationToNearbyPostcode() {
        checkNearby(BuryInterchange, Postcodes.CentralBury);
        checkNearby(ShudehillInterchange, Postcodes.NearShudehill);
    }

    private void checkNearby(Station start, PostcodeLocation end) {
        Set<Journey> journeys = planner.quickestRouteForLocation(txn, start,
                end.getLatLong(), createRequest(3)).collect(Collectors.toSet());

        assertFalse(journeys.isEmpty());

        journeys.forEach(journey -> {
            assertEquals(1, journey.getStages().size());
            assertEquals(TransportMode.Walk, journey.getStages().get(0).getMode());
            assertEquals(start, journey.getStages().get(0).getFirstStation());
        });
    }

    @Test
    void shouldPlanJourneyFromPostcodeToBusStation() {
        Set<Journey> journeys = planner.quickestRouteForLocation(txn, Postcodes.CentralBury.getLatLong(),
                BusStations.ShudehillInterchange, createRequest(5)).collect(Collectors.toSet());

        assertFalse(journeys.isEmpty());

        journeys.forEach(journey -> {
            assertEquals(TransportMode.Walk, journey.getStages().get(0).getMode());
            assertEquals(BusStations.ShudehillInterchange, getLastStage(journey).getLastStation());
        });
    }

    @Test
    void shouldPlanJourneyFromBusStationToPostcodeSouthbound() {
        Set<Journey> journeys = planner.quickestRouteForLocation(txn, BuryInterchange,
                Postcodes.NearShudehill.getLatLong(), createRequest(3)).collect(Collectors.toSet());

        int walkCost = CoordinateTransforms.calcCostInMinutes(Postcodes.NearShudehill.getLatLong(), BuryInterchange, testConfig.getWalkingMPH());

        assertTrue(walkCost<15);

        assertFalse(journeys.isEmpty());

        journeys.forEach(journey -> {
            assertEquals(BuryInterchange, journey.getStages().get(0).getFirstStation());
            assertEquals(Postcodes.NearShudehill.getLatLong(), getLastStage(journey).getLastStation().getLatLong());
        });

    }

    @Test
    void shouldPlanJourneyFromBusStationToPostcodeCentral() {
        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(day), time, false,
                10, 5).setDiag(true);
        Set<Journey> journeys = planner.quickestRouteForLocation(txn, BusStations.ShudehillInterchange,
                Postcodes.NearPiccadily.getLatLong(), journeyRequest).collect(Collectors.toSet());

        assertFalse(journeys.isEmpty());

        journeys.forEach(journey -> {
            assertEquals(2, journey.getStages().size());
            assertEquals(TransportMode.Bus, journey.getStages().get(0).getMode());
            assertEquals(Postcodes.NearPiccadily, getLastStage(journey).getLastStation());
        });
    }

    @Test
    void shouldPlanJourneyFromBusStationToPostcodeNorthbound() {
        Set<Journey> journeys = planner.quickestRouteForLocation(txn, BusStations.ShudehillInterchange,
                Postcodes.CentralBury.getLatLong(), createRequest(5)).collect(Collectors.toSet());

        assertFalse(journeys.isEmpty());

        journeys.forEach(journey -> {
            assertEquals(2, journey.getStages().size());
            assertEquals(TransportMode.Bus, journey.getStages().get(0).getMode());
            assertEquals(Postcodes.CentralBury, getLastStage(journey).getLastStation());
        });
    }

}

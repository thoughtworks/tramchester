package com.tramchester.integration.graph.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.resources.LocationJourneyPlanner;
import com.tramchester.testSupport.LocationJourneyPlannerTestFacade;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.reference.TestPostcodes;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.reference.BusStations.*;
import static org.junit.jupiter.api.Assertions.*;

@BusTest
class PostcodeBusRouteCalculatorTest {

    private static final int TXN_TIMEOUT = 5*60;

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static TramchesterConfig testConfig;

    private final TramDate day = TestEnv.testDay();
    private Transaction txn;
    private final TramTime time = TramTime.of(9,11);
    private LocationJourneyPlannerTestFacade planner;
    private Duration maxJourneyDuration;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        testConfig = new IntegrationBusTestConfig();
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
        database = componentContainer.get(GraphDatabase.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        txn = database.beginTx(TXN_TIMEOUT, TimeUnit.SECONDS);
        StationRepository stationRepository = componentContainer.get(StationRepository.class);

        planner = new LocationJourneyPlannerTestFacade(componentContainer.get(LocationJourneyPlanner.class), stationRepository, txn);
        maxJourneyDuration = Duration.ofMinutes(testConfig.getMaxJourneyDuration());
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldPlanJourneyFromPostcodeToPostcodeViaBusToPicc() {
        Set<Journey> journeys = planner.quickestRouteForLocation(TestPostcodes.CentralBury, TestPostcodes.NearPiccadillyGardens,
                new JourneyRequest(day, time, false, 2, maxJourneyDuration,
                        1, getRequestedModes()), 4);
        assertFalse(journeys.isEmpty(), "no journeys");
    }

    private Set<TransportMode> getRequestedModes() {
        return Collections.emptySet();
    }

    @Test
    void shouldPlanJourneyFromPostcodeToPostcodeViaBusToShudehill() {
        Set<Journey> journeys = planner.quickestRouteForLocation(TestPostcodes.CentralBury, TestPostcodes.NearShudehill,
                new JourneyRequest(day, time, false, 3, maxJourneyDuration,
                        1, getRequestedModes()), 6);
        assertFalse(journeys.isEmpty());
        assertWalkAtStart(journeys);
    }

    @Test
    void shouldWalkFromBusStationToNearbyPostcode() {
        checkNearby(StopAtShudehillInterchange, TestPostcodes.NearShudehill, new JourneyRequest(day, time, false,
                0, maxJourneyDuration, 3, getRequestedModes()));
    }

    @Test
    void shouldWalkFromPostcodeToBusStationNearby() {
        checkNearby(TestPostcodes.CentralBury, BuryInterchange);
        checkNearby(TestPostcodes.NearShudehill, StopAtShudehillInterchange);
    }

    @Test
    void shouldWalkFromPiccadillyGardensStopHToNearPiccadilyGardens() {
        final JourneyRequest journeyRequest = new JourneyRequest(day, time, false,
                0, maxJourneyDuration, 3, getRequestedModes());
        checkNearby(PiccadillyGardensStopN, TestPostcodes.NearPiccadillyGardens, journeyRequest);
    }

    @Test
    void shouldWalkFromNearPiccadilyGardensToPiccadillyGardensStopN() {
        checkNearby(TestPostcodes.NearPiccadillyGardens, PiccadillyGardensStopN);
    }

    @Test
    void shouldPlanJourneyFromBusStationToPostcodeSouthbound() {
        Set<Journey> journeys = planner.quickestRouteForLocation(BuryInterchange, TestPostcodes.NearShudehill,
                new JourneyRequest(day, time, false, 3, maxJourneyDuration,
                        1, getRequestedModes()), 4);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldPlanJourneyFromPostcodeToBusStation() {
        Set<Journey> journeys = planner.quickestRouteForLocation(TestPostcodes.CentralBury, StopAtShudehillInterchange,
                new JourneyRequest(day, time, false, 5, maxJourneyDuration,
                        1, getRequestedModes()), 6);
        assertFalse(journeys.isEmpty());
        assertWalkAtStart(journeys);
    }

    @Test
    void shouldPlanJourneyFromPostcodeToBusStationCentral() {
        JourneyRequest journeyRequest = new JourneyRequest(day, time, false, 3, maxJourneyDuration,
                1, getRequestedModes());
        Set<Journey> journeys = planner.quickestRouteForLocation(TestPostcodes.NearPiccadillyGardens, StopAtShudehillInterchange,
                journeyRequest, 4);

        assertFalse(journeys.isEmpty());
        assertWalkAtStart(journeys);
    }

    @Test
    void shouldPlanJourneyFromBusStationToPostcodeCentral() {
        JourneyRequest journeyRequest = new JourneyRequest(day, time, false, 2, maxJourneyDuration,
                1, getRequestedModes());
        Set<Journey> journeys = planner.quickestRouteForLocation(StopAtShudehillInterchange, TestPostcodes.NearPiccadillyGardens,
                journeyRequest, 3);

        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldPlanJourneyFromBusStationToPostcodeNorthbound() {
        Set<Journey> journeys = planner.quickestRouteForLocation(StopAtShudehillInterchange, TestPostcodes.CentralBury,
                new JourneyRequest(day, time, false, 1, maxJourneyDuration,
                        1, getRequestedModes()), 3);

        assertFalse(journeys.isEmpty());

        journeys.forEach(journey -> {
            List<TransportStage<?, ?>> stages = journey.getStages();
            final int size = stages.size();
            assertTrue(size<=3, journey.toString());
            assertEquals( TransportMode.Walk, stages.get(size-1).getMode(), journey.toString());
        });
    }

    @Test
    void shouldPlanJourneyFromPostcodeToPostcodesSouthbound() {
        Set<Journey> journeys = planner.quickestRouteForLocation(TestPostcodes.CentralBury, TestPostcodes.NearPiccadillyGardens,
                new JourneyRequest(day, time, false, 5, maxJourneyDuration,
                        1, getRequestedModes()), 6);

        assertFalse(journeys.isEmpty());

        journeys.forEach(journey -> {
            final List<TransportStage<?, ?>> stages = journey.getStages();
            assertEquals(3, stages.size(), journey.toString());
            assertEquals(TransportMode.Walk, stages.get(0).getMode());
            assertEquals(TransportMode.Bus, stages.get(1).getMode());
            assertEquals(TransportMode.Walk, stages.get(2).getMode());
        });
    }

    @Test
    void shouldPlanJourneyFromPostcodeToBusNorthbound() {
        final int maxChanges = 5;

        Set<Journey> journeys = planner.quickestRouteForLocation(TestPostcodes.NearShudehill, BuryInterchange,
                new JourneyRequest(day, time, false, maxChanges, maxJourneyDuration,
                        1, getRequestedModes()), 6);
        assertFalse(journeys.isEmpty());
        assertWalkAtStart(journeys);

        Set<Journey> fromPicc = planner.quickestRouteForLocation(TestPostcodes.NearPiccadillyGardens, BuryInterchange,
                new JourneyRequest(day, time, false, maxChanges, maxJourneyDuration,
                        1, getRequestedModes()), 6);
        assertFalse(fromPicc.isEmpty());
        assertWalkAtStart(fromPicc);
    }

    private void assertWalkAtStart(Set<Journey> journeys) {
        journeys.forEach(journey -> assertEquals(TransportMode.Walk, journey.getStages().get(0).getMode()));
    }

    private void checkNearby(PostcodeLocation start, BusStations end) {
        JourneyRequest request = new JourneyRequest(day, time, false, 3,
                maxJourneyDuration, 1, getRequestedModes());

        Set<Journey> journeys = planner.quickestRouteForLocation(start, end, request, 4);
        assertFalse(journeys.isEmpty(), "no journeys");

        Set<Journey> oneStage = journeys.stream().filter(Journey::isDirect).collect(Collectors.toSet());
        assertFalse(oneStage.isEmpty(), "no direct journey");

        oneStage.forEach(journey -> {
            TransportStage<?,?> transportStage = journey.getStages().get(0);
            assertEquals(TransportMode.Walk, transportStage.getMode());
            assertEquals(start.getLatLong(), transportStage.getFirstStation().getLatLong());
            assertEquals(end.getId(), transportStage.getLastStation().getId());
        });
    }

    private void checkNearby(BusStations start, PostcodeLocation end, JourneyRequest journeyRequest) {

        Set<Journey> journeys = planner.quickestRouteForLocation(start, end, journeyRequest, 3);
        assertFalse(journeys.isEmpty(), "no journeys");

        Set<Journey> oneStage = journeys.stream().filter(Journey::isDirect).collect(Collectors.toSet());
        assertFalse(oneStage.isEmpty(), "Missing one stage journey, got " + journeys);

        oneStage.forEach(journey -> {
            final TransportStage<?, ?> transportStage = journey.getStages().get(0);
            assertEquals(TransportMode.Walk, transportStage.getMode());
            assertEquals(start.getId(), transportStage.getFirstStation().getId());
        });
    }

}

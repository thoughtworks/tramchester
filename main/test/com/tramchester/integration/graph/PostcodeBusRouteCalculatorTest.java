package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.repository.StationRepository;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class PostcodeBusRouteCalculatorTest {
    // TODO this needs to be > time for whole test fixture, see note below in @After
    private static final int TXN_TIMEOUT = 5*60;

    private static Dependencies dependencies;
    private static GraphDatabase database;
    private static TramchesterConfig testConfig;

    private final LocalDate day = TestEnv.testDay();
    private Transaction txn;
    private final TramTime time = TramTime.of(9,11);
    private LocationJourneyPlannerTestFacade planner;

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
        StationRepository stationRepository = dependencies.get(StationRepository.class);

        planner = new LocationJourneyPlannerTestFacade(dependencies.get(LocationJourneyPlanner.class), stationRepository, txn);
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldPlanJourneyFromPostcodeToPostcodeViaBusToPicc() {
        Set<Journey> journeys = planner.quickestRouteForLocation(Postcodes.CentralBury, Postcodes.NearPiccadilyGardens, createRequest(3));
        assertFalse(journeys.isEmpty());
        assertWalkAtStart(journeys);
    }

    @Test
    void shouldPlanJourneyFromPostcodeToPostcodeViaBusToShudehill() {
        Set<Journey> journeys = planner.quickestRouteForLocation(Postcodes.CentralBury, Postcodes.NearShudehill, createRequest(5));
        assertFalse(journeys.isEmpty());
        assertWalkAtStart(journeys);
    }

    @Test
    void shouldWalkFromBusStationToNearbyPostcode() {
        checkNearby(BuryInterchange, Postcodes.CentralBury);
        checkNearby(ShudehillInterchange, Postcodes.NearShudehill);
        checkNearby(PiccadillyGardensStopH, Postcodes.NearPiccadilyGardens);
    }

    @Test
    void shouldWalkFromPostcodeToBusStationNearby() {
        checkNearby(Postcodes.CentralBury, BuryInterchange);
        checkNearby(Postcodes.NearShudehill, ShudehillInterchange);
        checkNearby(Postcodes.NearPiccadilyGardens, PiccadillyGardensStopH);
    }

    @Test
    void shouldPlanJourneyFromBusStationToPostcodeSouthbound() {
        Set<Journey> journeys = getJourneys(BuryInterchange, Postcodes.NearShudehill, createRequest(3));
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldPlanJourneyFromPostcodeToBusStation() {
        Set<Journey> journeys = getJourneys(Postcodes.CentralBury, ShudehillInterchange, createRequest(5));
        assertFalse(journeys.isEmpty());
        assertWalkAtStart(journeys);
    }


    @Test
    void shouldPlanJourneyFromPostcodeToBusStationCentral() {
        JourneyRequest journeyRequest = createRequest(3);
        Set<Journey> journeys = getJourneys(Postcodes.NearPiccadilyGardens, ShudehillInterchange, journeyRequest);

        assertFalse(journeys.isEmpty());
        assertWalkAtStart(journeys);
    }

    @Test
    void shouldPlanJourneyFromBusStationToPostcodeCentral() {
        JourneyRequest journeyRequest = createRequest(3);
        Set<Journey> journeys = getJourneys(ShudehillInterchange, Postcodes.NearPiccadilyGardens, journeyRequest);

        assertFalse(journeys.isEmpty());
        assertBusAtStart(journeys);
    }

    @Test
    void shouldPlanJourneyFromBusStationToPostcodeNorthbound() {
        Set<Journey> journeys = getJourneys(ShudehillInterchange, Postcodes.CentralBury, createRequest(5));

        assertFalse(journeys.isEmpty());

        assertBusAtStart(journeys);
        journeys.forEach(journey -> {
            assertEquals(2, journey.getStages().size());
        });
    }

    @Test
    void shouldPlanJourneyFromPostcodeToBusNorthbound() {
        Set<Journey> journeys = getJourneys(Postcodes.NearShudehill, BuryInterchange, createRequest(5));
        assertFalse(journeys.isEmpty());
        assertWalkAtStart(journeys);

        Set<Journey> fromPicc = getJourneys(Postcodes.NearPiccadilyGardens, BuryInterchange, createRequest(5));
        assertFalse(fromPicc.isEmpty());
        assertWalkAtStart(fromPicc);
    }

    @NotNull
    private JourneyRequest createRequest(int maxChanges) {
        return new JourneyRequest(new TramServiceDate(day), time, false, maxChanges, testConfig.getMaxJourneyDuration());
    }

    private void assertBusAtStart(Set<Journey> journeys) {
        journeys.forEach(journey -> assertEquals(TransportMode.Bus, journey.getStages().get(0).getMode()));
    }

    private void assertWalkAtStart(Set<Journey> journeys) {
        journeys.forEach(journey -> assertEquals(TransportMode.Walk, journey.getStages().get(0).getMode()));
    }


    @NotNull
    private Set<Journey> getJourneys(BusStations start, PostcodeLocation end, JourneyRequest request) {
        return planner.quickestRouteForLocation(start, end, request);
//        Stream<Journey> journeyStream = planner.
//                quickestRouteForLocation(txn, TestStation.real(stationRepository, start), end.getLatLong(), request);
//        Set<Journey> result = journeyStream.collect(Collectors.toSet());
//        journeyStream.close();
//        return result;
    }

    private Set<Journey> getJourneys(PostcodeLocation start, BusStations end, JourneyRequest request) {
        return planner.quickestRouteForLocation(start, end, request);
    }

    private void checkNearby(PostcodeLocation start, BusStations end) {
        JourneyRequest request = createRequest(3);
        Set<Journey> journeys = getJourneys(start, end, request);

        assertFalse(journeys.isEmpty(), "no journeys");

        Set<Journey> oneStage = journeys.stream().filter(journey->journey.getStages().size()==1).collect(Collectors.toSet());
        assertFalse(oneStage.isEmpty(), "no one stage journey");

        oneStage.forEach(journey -> {
            assertEquals(1, journey.getStages().size());
            TransportStage transportStage = journey.getStages().get(0);
            assertEquals(TransportMode.Walk, transportStage.getMode());
            assertEquals(start.getLatLong(), transportStage.getFirstStation().getLatLong());
            assertEquals(end.getId(), transportStage.getLastStation().getId());
        });
    }

    private void checkNearby(BusStations start, PostcodeLocation end) {
        JourneyRequest request = createRequest(1);
        Set<Journey> journeys = getJourneys(start, end, request);

        assertFalse(journeys.isEmpty(), "no journeys");
        Set<Journey> oneStage = journeys.stream().filter(journey->journey.getStages().size()==1).collect(Collectors.toSet());
        assertFalse(oneStage.isEmpty(), "no one stage journey");

        oneStage.forEach(journey -> {
            assertEquals(1, journey.getStages().size());
            assertEquals(TransportMode.Walk, journey.getStages().get(0).getMode());
            assertEquals(start.getId(), journey.getStages().get(0).getFirstStation().getId());
        });
    }

}

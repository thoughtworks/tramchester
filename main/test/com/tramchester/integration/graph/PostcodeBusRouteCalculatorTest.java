package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.reference.TransportMode;
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
import java.util.List;
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
        Set<Journey> journeys = planner.quickestRouteForLocation(Postcodes.CentralBury, Postcodes.NearPiccadillyGardens,
                createRequest(5), 7);
        assertFalse(journeys.isEmpty(), "no journeys");
    }

    @Test
    void shouldPlanJourneyFromPostcodeToPostcodeViaBusToShudehill() {
        Set<Journey> journeys = planner.quickestRouteForLocation(Postcodes.CentralBury, Postcodes.NearShudehill,
                createRequest(5), 6);
        assertFalse(journeys.isEmpty());
        assertWalkAtStart(journeys);
    }

    @Test
    void shouldWalkFromBusStationToNearbyPostcode() {
        checkNearby(BuryInterchange, Postcodes.CentralBury);
        checkNearby(ShudehillInterchange, Postcodes.NearShudehill);
    }

    @Test
    void shouldWalkFromPostcodeToBusStationNearby() {
        checkNearby(Postcodes.CentralBury, BuryInterchange);
        checkNearby(Postcodes.NearShudehill, ShudehillInterchange);
    }

    @Test
    void shouldWalkFromPiccadillyGardensStopHToNearPiccadilyGardens() {
        checkNearby(PiccadillyGardensStopN, Postcodes.NearPiccadillyGardens);
    }

    @Test
    void shouldWalkFromNearPiccadilyGardensToPiccadillyGardensStopH() {
        checkNearby(Postcodes.NearPiccadillyGardens, PiccadillyGardensStopN);
    }

    @Test
    void shouldPlanJourneyFromBusStationToPostcodeSouthbound() {
        Set<Journey> journeys = planner.quickestRouteForLocation(BuryInterchange, Postcodes.NearShudehill,
                createRequest(3), 4);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldPlanJourneyFromPostcodeToBusStation() {
        Set<Journey> journeys = planner.quickestRouteForLocation(Postcodes.CentralBury, ShudehillInterchange,
                createRequest(5), 6);
        assertFalse(journeys.isEmpty());
        assertWalkAtStart(journeys);
    }


    @Test
    void shouldPlanJourneyFromPostcodeToBusStationCentral() {
        JourneyRequest journeyRequest = createRequest(3);
        Set<Journey> journeys = planner.quickestRouteForLocation(Postcodes.NearPiccadillyGardens, ShudehillInterchange, journeyRequest, 4);

        assertFalse(journeys.isEmpty());
        assertWalkAtStart(journeys);
    }

    @Test
    void shouldPlanJourneyFromBusStationToPostcodeCentral() {
        JourneyRequest journeyRequest = createRequest(2);
        Set<Journey> journeys = planner.quickestRouteForLocation(ShudehillInterchange, Postcodes.NearPiccadillyGardens, journeyRequest, 3);

        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldPlanJourneyFromBusStationToPostcodeNorthbound() {
        Set<Journey> journeys = planner.quickestRouteForLocation(ShudehillInterchange, Postcodes.CentralBury,
                createRequest(2), 3);

        assertFalse(journeys.isEmpty());

        journeys.forEach(journey -> {
            // connecting stage first as bus only
            List<TransportStage<?, ?>> stages = journey.getStages();
            int size = stages.size();

            assertEquals( TransportMode.Connect, stages.get(0).getMode());
            assertEquals( TransportMode.Walk, stages.get(size-1).getMode());
            assertEquals(3, size);
        });
    }

    @Test
    void shouldPlanJourneyFromPostcodeToPostcodesSouthbound() {
        Set<Journey> journeys = planner.quickestRouteForLocation(Postcodes.CentralBury, Postcodes.NearPiccadillyGardens,
                createRequest(5), 6);

        assertFalse(journeys.isEmpty());

        journeys.forEach(journey -> {
            assertEquals(TransportMode.Walk, journey.getStages().get(0).getMode());
            assertEquals(1, journey.getStages().size(), journey.toString());
            //assertEquals(BusStations.BuryInterchange.getId().forDTO(), journey.getEnd().getId());
        });
    }

    @Test
    void shouldPlanJourneyFromPostcodeToBusNorthbound() {
        Set<Journey> journeys = planner.quickestRouteForLocation(Postcodes.NearShudehill, BuryInterchange,
                createRequest(5), 6);
        assertFalse(journeys.isEmpty());
        assertWalkAtStart(journeys);

        Set<Journey> fromPicc = planner.quickestRouteForLocation(Postcodes.NearPiccadillyGardens, BuryInterchange,
                createRequest(5), 6);
        assertFalse(fromPicc.isEmpty());
        assertWalkAtStart(fromPicc);
    }

    @NotNull
    private JourneyRequest createRequest(int maxChanges) {
        return new JourneyRequest(new TramServiceDate(day), time, false, maxChanges, testConfig.getMaxJourneyDuration());
    }

    private void assertWalkAtStart(Set<Journey> journeys) {
        journeys.forEach(journey -> assertEquals(TransportMode.Walk, journey.getStages().get(0).getMode()));
    }

    private void checkNearby(PostcodeLocation start, BusStations end) {
        JourneyRequest request = createRequest(3);
        Set<Journey> journeys = planner.quickestRouteForLocation(start, end, request, 4);

        assertFalse(journeys.isEmpty(), "no journeys");

        Set<Journey> oneStage = journeys.stream().filter(journey->journey.getStages().size()==1).collect(Collectors.toSet());
        assertFalse(oneStage.isEmpty(), "no one stage journey");

        oneStage.forEach(journey -> {
            assertEquals(1, journey.getStages().size());
            TransportStage<?,?> transportStage = journey.getStages().get(0);
            assertEquals(TransportMode.Walk, transportStage.getMode());
            assertEquals(start.getLatLong(), transportStage.getFirstStation().getLatLong());
            assertEquals(end.getId(), transportStage.getLastStation().getId());
        });
    }

    private void checkNearby(BusStations start, PostcodeLocation end) {
        JourneyRequest request = createRequest(1);
        Set<Journey> journeys = planner.quickestRouteForLocation(start, end, request, 3);

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

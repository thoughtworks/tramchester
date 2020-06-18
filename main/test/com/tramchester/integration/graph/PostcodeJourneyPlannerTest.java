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
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.PostcodeRepository;
import com.tramchester.resources.LocationJourneyPlanner;
import com.tramchester.testSupport.Postcodes;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.WithPostcodesEnabled;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;


class PostcodeJourneyPlannerTest {

    // TODO WIP

    private static final int TXN_TIMEOUT = 5*60;

    private static Dependencies dependencies;
    private static GraphDatabase database;

    private static final LocalDate when = TestEnv.testDay();
    private Transaction txn;
    private LocationJourneyPlanner planner;
    private PostcodeRepository repository;
    private PostcodeLocation centralLocation;
    private static final TramTime planningTime = TramTime.of(11, 42);

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        TramchesterConfig testConfig = new WithPostcodesEnabled();
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
        repository = dependencies.get(PostcodeRepository.class);
        //request = new JourneyRequest(new TramServiceDate(nextTuesday), planningTime, arriveBy);
        centralLocation = repository.getPostcode(Postcodes.NearPiccadily);
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    private static Stream<JourneyRequest> getRequest() {
        return Stream.of(
                new JourneyRequest(new TramServiceDate(when), planningTime, false),
                new JourneyRequest(new TramServiceDate(when), planningTime, true));
    }

    @ParameterizedTest
    @MethodSource("getRequest")
    void shouldHaveJourneyFromCentralPostcodeToBury(JourneyRequest request) {
        Stream<Journey> journeyStream = planner.quickestRouteForLocation(txn, centralLocation.getLatLong(),
                Stations.Bury, request);

        Set<Journey> journeySet = journeyStream.collect(Collectors.toSet());
        journeyStream.close();

        assertFalse(journeySet.isEmpty());
        journeySet.forEach(journey -> assertEquals(TransportMode.Walk, journey.getStages().get(0).getMode()));
        checkDepartBefore(journeySet, request.getArriveBy());
    }

    @ParameterizedTest
    @MethodSource("getRequest")
    void shouldHaveJourneyFromBuryToCentralPostcode(JourneyRequest request) {
        Stream<Journey> journeyStream = planner.quickestRouteForLocation(txn, Stations.Bury,
                centralLocation.getLatLong(), request);

        Set<Journey> journeySet = journeyStream.collect(Collectors.toSet());
        journeyStream.close();

        assertFalse(journeySet.isEmpty());
        journeySet.forEach(journey -> assertEquals(TransportMode.Tram, journey.getStages().get(0).getMode()));
        checkDepartBefore(journeySet, request.getArriveBy());

    }

    @ParameterizedTest
    @MethodSource("getRequest")
    void shouldHavePostcodeToPostcodeJourney(JourneyRequest request) {
        PostcodeLocation buryPostcode = repository.getPostcode(Postcodes.CentralBury);
        Stream<Journey> journeyStream = planner.quickestRouteForLocation(txn, centralLocation.getLatLong(),
                buryPostcode.getLatLong(), request);

        Set<Journey> journeySet = journeyStream.collect(Collectors.toSet());
        journeyStream.close();

        assertFalse(journeySet.isEmpty());
        journeySet.forEach(journey -> assertTrue(journey.getStages().size()>=3));
        // walk at start
        journeySet.forEach(journey -> assertEquals(TransportMode.Walk, journey.getStages().get(0).getMode()));
        // walk at end
        journeySet.forEach(journey -> {
            List<TransportStage> stages = journey.getStages();
            int lastIndex = stages.size()-1;
            assertEquals(TransportMode.Walk, stages.get(lastIndex).getMode());
        });
        // trams in the middle
        journeySet.forEach(journey -> {
            List<TransportStage> stages = journey.getStages();
            for (int i = 1; i < stages.size()-1; i++) {
                assertEquals(TransportMode.Tram, stages.get(i).getMode());
            }
        });
        checkDepartBefore(journeySet, request.getArriveBy());
    }

    private void checkDepartBefore(Set<Journey> journeySet, boolean arriveBy) {
        if (arriveBy) {
            journeySet.forEach(journey -> journey.getStages().get(0).getFirstDepartureTime().isBefore(planningTime));
        }
    }
}

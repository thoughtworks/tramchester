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
import com.tramchester.resources.PostcodeJourneyPlanner;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestEnv;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;


@RunWith(Parameterized.class)
public class PostcodeJourneyPlannerTest {

    // TODO WIP

    private static final int TXN_TIMEOUT = 5*60;

    private static Dependencies dependencies;
    private static GraphDatabase database;

    private final LocalDate nextTuesday = TestEnv.nextTuesday(0);
    private Transaction tx;
    private PostcodeJourneyPlanner planner;
    private PostcodeRepository repository;
    private JourneyRequest request;
    private PostcodeLocation centralLocation;
    private TramTime planningTime;

    @Parameterized.Parameters(name="{index}: {0}")
    public static Iterable<Boolean> data() {
        return Arrays.asList(true, false);
    }

    @Parameterized.Parameter
    public Boolean arriveBy;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        TramchesterConfig testConfig = new IntegrationTramTestConfig();
        dependencies.initialise(testConfig);
        database = dependencies.get(GraphDatabase.class);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Before
    public void beforeEachTestRuns() {
        tx = database.beginTx(TXN_TIMEOUT, TimeUnit.SECONDS);
        planner = dependencies.get(PostcodeJourneyPlanner.class);
        repository = dependencies.get(PostcodeRepository.class);
        planningTime = TramTime.of(11, 42);
        request = new JourneyRequest(new TramServiceDate(nextTuesday), planningTime, arriveBy);
        centralLocation = repository.getPostcode("M139WL");

    }

    @After
    public void afterEachTestRuns() {
        tx.close();
    }

    @Test
    public void shouldHaveJourneyFromCentralPostcodeToBury() {
        Stream<Journey> journeyStream = planner.quickestRouteForLocation(centralLocation, Stations.Bury, request);

        Set<Journey> journeySet = journeyStream.collect(Collectors.toSet());
        journeyStream.close();

        assertFalse(journeySet.isEmpty());
        journeySet.forEach(journey -> assertEquals(TransportMode.Walk, journey.getStages().get(0).getMode()));
        checkDepartBefore(journeySet);
    }

    @Test
    public void shouldHaveJourneyFromBuryToCentralPostcode() {
        Stream<Journey> journeyStream = planner.quickestRouteForLocation(Stations.Bury, centralLocation, request);

        Set<Journey> journeySet = journeyStream.collect(Collectors.toSet());
        journeyStream.close();

        assertFalse(journeySet.isEmpty());
        journeySet.forEach(journey -> assertEquals(TransportMode.Tram, journey.getStages().get(0).getMode()));
        checkDepartBefore(journeySet);

    }

    @Test
    public void shouldHavePostcodeToPostcodeJourney() {
        PostcodeLocation buryPostcode = repository.getPostcode("BL90AY");
        Stream<Journey> journeyStream = planner.quickestRouteForLocation(centralLocation, buryPostcode, request);

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
        checkDepartBefore(journeySet);
    }

    private void checkDepartBefore(Set<Journey> journeySet) {
        if (arriveBy) {
            journeySet.forEach(journey -> journey.getStages().get(0).getFirstDepartureTime().isBefore(planningTime));
        }
    }
}

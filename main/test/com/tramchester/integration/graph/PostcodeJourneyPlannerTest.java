package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.PostcodeRepository;
import com.tramchester.resources.LocationJourneyPlanner;
import com.tramchester.resources.PostcodeJourneyPlanner;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestEnv;
import org.junit.*;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;


public class PostcodeJourneyPlannerTest {

    // TODO WIP

    private static final int TXN_TIMEOUT = 5*60;

    private static Dependencies dependencies;
    private static TramchesterConfig testConfig;
    private static GraphDatabase database;

    private final LocalDate nextTuesday = TestEnv.nextTuesday(0);
    private Transaction tx;
    private PostcodeJourneyPlanner planner;
    private PostcodeRepository repository;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        testConfig = new IntegrationTramTestConfig();
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
    }

    @After
    public void afterEachTestRuns() {
        tx.close();
    }

    @Test
    public void shouldHaveJourneyFromCentralPostcodeToBury() {
        JourneyRequest request = new JourneyRequest(new TramServiceDate(nextTuesday), TramTime.of(11,42), false);
        PostcodeLocation location = repository.getPostcode("M139WL");
        Stream<Journey> journeyStream = planner.quickestRouteForLocation(location, Stations.Bury, request);

        Set<Journey> journeySet = journeyStream.collect(Collectors.toSet());
        journeyStream.close();

        assertFalse(journeySet.isEmpty());
        journeySet.forEach(journey -> {
            assertEquals(TransportMode.Walk, journey.getStages().get(0).getMode());
        });
    }

    @Test
    public void shouldHaveJourneyFromBuryToCentralPostcode() {
        JourneyRequest request = new JourneyRequest(new TramServiceDate(nextTuesday), TramTime.of(11,42), false);
        PostcodeLocation location = repository.getPostcode("M139WL");
        Stream<Journey> journeyStream = planner.quickestRouteForLocation(Stations.Bury, location, request);

        Set<Journey> journeySet = journeyStream.collect(Collectors.toSet());
        journeyStream.close();

        assertFalse(journeySet.isEmpty());
        journeySet.forEach(journey -> {
            assertEquals(TransportMode.Tram, journey.getStages().get(0).getMode());
        });
    }
}

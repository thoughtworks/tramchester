package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.integration.IntegrationBusTestConfig;
import com.tramchester.repository.PostcodeRepository;
import com.tramchester.resources.LocationJourneyPlanner;
import com.tramchester.testSupport.BusTest;
import com.tramchester.testSupport.TestEnv;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Ignore("WIP")
public class RouteCalculatorFromPostcodeSpike {
    private static Dependencies dependencies;
    private static GraphDatabase database;

    private final LocalDate nextTuesday = TestEnv.nextTuesday(0);
    private Transaction tx;
    private LocationJourneyPlanner locationJourneyPlanner;
    private PostcodeRepository postCodeRepository;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        TramchesterConfig testConfig = new IntegrationBusTestConfig();
        dependencies.initialise(testConfig);
        database = dependencies.get(GraphDatabase.class);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Before
    public void beforeEachTestRuns() {
        tx = database.beginTx(60, TimeUnit.SECONDS);
        locationJourneyPlanner = dependencies.get(LocationJourneyPlanner.class);
        postCodeRepository = dependencies.get(PostcodeRepository.class);
    }

    @After
    public void afterEachTestRuns() {
        tx.close();
    }

    @Category(BusTest.class)
    @Test
    public void shouldSpikePostcodeToPostcode() {
        int count = postCodeRepository.getNumberOf();

        ArrayList<Pair> failures = new ArrayList<>();
        ArrayList<Pair> combinations = new ArrayList<>(count);

        Collection<PostcodeLocation> allPostcodes = postCodeRepository.getPostcodes();
        allPostcodes.forEach(begin -> allPostcodes.forEach(dest -> {
            if (!begin.equals(dest)) {
                combinations.add(Pair.of(begin, dest));
            }
        }));
        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(nextTuesday), TramTime.of(8,45), false);
        for (Pair locations : combinations) {
            Stream<Journey> journeys = locationJourneyPlanner.quickestRouteForLocation(locations.begin.getLatLong(),
                    locations.dest.getLatLong(),
                    journeyRequest);
            if (journeys.findAny().isEmpty()) {
                // TOOD tmp
                fail("Not journey between " + locations);

                failures.add(locations);
            }
            journeys.close();
        }
        assertTrue(failures.toString(), failures.isEmpty());
    }

    private static class Pair {
        private final PostcodeLocation begin;
        private final PostcodeLocation dest;

        private static Pair of(PostcodeLocation begin, PostcodeLocation dest) {
            return new Pair(begin, dest);
        }

        private Pair(PostcodeLocation begin, PostcodeLocation dest) {
            this.begin = begin;
            this.dest = dest;
        }
    }
}

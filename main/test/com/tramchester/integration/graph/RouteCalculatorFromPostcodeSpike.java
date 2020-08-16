package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
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
import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class RouteCalculatorFromPostcodeSpike {
    private static Dependencies dependencies;
    private static GraphDatabase database;
    private static IntegrationBusTestConfig testConfig;

    private final LocalDate when = TestEnv.testDay();
    private Transaction txn;
    private LocationJourneyPlanner locationJourneyPlanner;
    private PostcodeRepository postCodeRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
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
        txn = database.beginTx(60, TimeUnit.SECONDS);
        locationJourneyPlanner = dependencies.get(LocationJourneyPlanner.class);
        postCodeRepository = dependencies.get(PostcodeRepository.class);
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Category(BusTest.class)
    @Test
    void shouldSpikePostcodeToPostcode() {
        int count = postCodeRepository.getNumberOf();

        ArrayList<Pair> failures = new ArrayList<>();
        ArrayList<Pair> combinations = new ArrayList<>(count);

        Collection<PostcodeLocation> allPostcodes = postCodeRepository.getPostcodes();
        allPostcodes.forEach(begin -> allPostcodes.forEach(dest -> {
            if (!begin.equals(dest)) {
                combinations.add(Pair.of(begin, dest));
            }
        }));
        JourneyRequest journeyRequest = new JourneyRequest(
                new TramServiceDate(when), TramTime.of(8,45), false, 8, testConfig.getMaxJourneyDuration());
        for (Pair locations : combinations) {
            Stream<Journey> journeys = locationJourneyPlanner.quickestRouteForLocation(txn, locations.begin.getLatLong(),
                    locations.dest.getLatLong(),
                    journeyRequest);
            if (journeys.findAny().isEmpty()) {
                // TOOD tmp
                Assertions.fail("Not journey between " + locations);

                failures.add(locations);
            }
            journeys.close();
        }
        //noinspection ConstantConditions
        Assertions.assertTrue(failures.isEmpty(), failures.toString());
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

package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestEnv;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.TestEnv.avoidChristmasDate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("JUnitTestMethodWithNoAssertions")
public class RouteCalulatorTestKeyRoutes {


    private static Dependencies dependencies;
    private static TramchesterConfig testConfig;
    private static GraphDatabase database;

    private RouteCalculator calculator;
    private final LocalDate nextTuesday = TestEnv.nextTuesday(0);

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        testConfig = new IntegrationTramTestConfig();
        dependencies.initialise(testConfig);
        database = dependencies.get(GraphDatabase.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        calculator = dependencies.get(RouteCalculator.class);
    }

    @Test
    void shouldFindInterchangesToEndOfLines() {
        Set<Pair<Station, Station>> combinations = createJourneyPairs(Stations.Interchanges,Stations.EndOfTheLine );
        checkRouteNextNDays(combinations, nextTuesday, TramTime.of(8,0), 7);
    }


    @Test
    void shouldFindEndOfLinesToEndOfLines() {
        // todo: changed from 9 to 10.15 as airport to eccles fails for 10.15am
        Set<Pair<Station, Station>> combinations = createJourneyPairs(Stations.EndOfTheLine, Stations.EndOfTheLine);
        checkRouteNextNDays(combinations, nextTuesday, TramTime.of(10,15), 7);
    }

    @Test
    void shouldFindInterchangesToInterchanges() {
        Set<Pair<Station, Station>> combinations = createJourneyPairs(Stations.Interchanges, Stations.Interchanges);
        checkRouteNextNDays(combinations, nextTuesday, TramTime.of(9,0), 7);
    }

    @Test
    void shouldFindEndOfLinesToInterchanges() {
        Set<Pair<Station, Station>> combinations = createJourneyPairs(Stations.EndOfTheLine, Stations.Interchanges);
        checkRouteNextNDays(combinations, nextTuesday, TramTime.of(9,0), 7);
    }

    @Test
    @Disabled
    void shouldFindEndOfLinesToEndOfLinesFindLongestDuration() {
        Set<Pair<Station, Station>> combinations = createJourneyPairs(Stations.EndOfTheLine, Stations.EndOfTheLine);

        List<Journey> allResults = new ArrayList<>();

        Map<Pair<Station, Station>, JourneyOrNot> results = validateAllHaveAtLeastOneJourney(nextTuesday,
                combinations, TramTime.of(9,0));
        results.forEach((route, journey) -> journey.ifPresent(allResults::add));

        double longest = allResults.stream().map(RouteCalculatorTest::costOfJourney).max(Integer::compare).get();
        assertEquals(testConfig.getMaxJourneyDuration(), longest, 0.001);

    }

    private void checkRouteNextNDays(Set<Pair<Station,Station>> combinations, LocalDate date, TramTime time, int numDays) {

        for(int day = 0; day< numDays; day++) {
            LocalDate testDate = avoidChristmasDate(date.plusDays(day));
            validateAllHaveAtLeastOneJourney(testDate, combinations, time);
        }
    }

    private Map<Pair<Station, Station>, JourneyOrNot> validateAllHaveAtLeastOneJourney(
            LocalDate queryDate, Set<Pair<Station, Station>> combinations, TramTime queryTime) {

        // check each pair, collect results into (station,station)->result
        Map<Pair<Station, Station>, JourneyOrNot> results =
                combinations.parallelStream().
                        map(requested -> {
                            try (Transaction txn = database.beginTx()) {
                                Optional<Journey> optionalJourney = calculator.calculateRoute(txn, requested.getLeft(), requested.getRight(),
                                        new JourneyRequest(new TramServiceDate(queryDate), queryTime, false)).limit(1).findAny();
                                JourneyOrNot journeyOrNot = new JourneyOrNot(requested, queryDate, queryTime, optionalJourney);
                                return Pair.of(requested, journeyOrNot);
                            }
                        }).
                        collect(Collectors.toMap(Pair::getLeft, Pair::getRight));

        // check all results present, collect failures into a list
        List<JourneyOrNot> failed = results.entrySet().stream().
                filter(journeyOrNot -> journeyOrNot.getValue().missing()).
                map(Map.Entry::getValue).
                collect(Collectors.toList());
        assertTrue(failed.isEmpty(), "missing routes: " + failed.toString());
        return results;
    }

    private Set<Pair<Station, Station>> createJourneyPairs(List<Station> starts, List<Station> ends) {
        Set<Pair<Station, Station>> combinations = new HashSet<>();
        for (Station start : starts) {
            for (Station dest : ends) {
                if (!dest.equals(start)) {
                    combinations.add(Pair.of(start, dest));
                }
            }
        }
        return combinations;
    }

    private static class JourneyOrNot {
        private final Pair<Station, Station> requested;
        private final LocalDate queryDate;
        private final TramTime queryTime;
        private final Journey journey;

        public JourneyOrNot(Pair<Station, Station> requested, LocalDate queryDate, TramTime queryTime,
                            Optional<Journey> optionalJourney) {
            this.requested = requested;
            this.queryDate = queryDate;
            this.queryTime = queryTime;
            this.journey = optionalJourney.orElse(null);
        }

        public boolean missing() {
            return journey==null;
        }

        public void ifPresent(Consumer<Journey> action) {
            if (this.journey != null) {
                action.accept(this.journey);
            }
        }

        @Override
        public String toString() {
            return "JourneyOrNot{" +
                    " queryDate=" + queryDate +
                    ", queryTime=" + queryTime +
                    ", requested=" + requested +
                    '}';
        }
    }

}

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
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.DataExpiryCategory;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramStations;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.TestEnv.avoidChristmasDate;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("JUnitTestMethodWithNoAssertions")
class RouteCalulatorTestKeyRoutes {

    private static Dependencies dependencies;
    private static TramchesterConfig testConfig;
    private static GraphDatabase database;

    private RouteCalculator calculator;
    private final LocalDate when = TestEnv.testDay();
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
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
        stationRepository = dependencies.get(StationRepository.class);
        calculator = dependencies.get(RouteCalculator.class);
    }

    @Test
    void shouldFindInterchangesToEndOfLines() {
        Set<TramStations.Pair> combinations = createJourneyPairs(TramStations.Interchanges, TramStations.EndOfTheLine );

        validateAllHaveAtLeastOneJourney(when, combinations, TramTime.of(8,0));
    }

    @Disabled("used for diagnosing specific issue")
    @Test
    void shouldRepoServiceTimeIssueForConcurrency() {
        List<TramStations.Pair> combinations = new ArrayList<>();
        for (int i = 0; i < 99; i++) {
            combinations.add(new TramStations.Pair(TramStations.ShawAndCrompton, TramStations.Ashton));
        }

        LocalDate queryDate = when;
        TramTime queryTime = TramTime.of(8,0);
        boolean diag = true;

        Optional<Pair<TramStations.Pair, JourneyOrNot>> failed = combinations.parallelStream().
                map(requested -> {
                    try (Transaction txn = database.beginTx()) {
                        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(queryDate), queryTime, false,
                                3, testConfig.getMaxJourneyDuration());
                        journeyRequest.setDiag(diag);
                        Optional<Journey> optionalJourney = calculator.calculateRoute(txn,
                                TramStations.real(stationRepository, requested.getStart()),
                                TramStations.real(stationRepository, requested.getDest()),
                                journeyRequest).limit(1).findAny();
                        JourneyOrNot journeyOrNot = new JourneyOrNot(requested, queryDate, queryTime, optionalJourney);
                        return Pair.of(requested, journeyOrNot);
                    }
                }).filter(pair -> pair.getRight().missing()).findAny();

        assertFalse(failed.isPresent());
    }

    @Test
    void shouldFindEndOfLinesToInterchanges() {
        Set<TramStations.Pair> combinations = createJourneyPairs(TramStations.EndOfTheLine, TramStations.Interchanges);
        validateAllHaveAtLeastOneJourney(when, combinations, TramTime.of(8,0));
    }

    @Test
    void shouldFindInterchangesToInterchanges() {
        Set<TramStations.Pair> combinations = createJourneyPairs(TramStations.Interchanges, TramStations.Interchanges);
        validateAllHaveAtLeastOneJourney(when, combinations, TramTime.of(8,0));
    }

    @DataExpiryCategory
    @Test
    void shouldFindEndOfLinesToEndOfLinesNextNDays() {
        // todo: changed from 9 to 10.15 as airport to eccles fails for 10.15am
        Set<TramStations.Pair> combinations = createJourneyPairs(TramStations.EndOfTheLine, TramStations.EndOfTheLine);
        checkRouteNextNDays(combinations, when, TramTime.of(10,15));
    }

    @Test
    @Disabled
    void shouldFindEndOfLinesToEndOfLinesFindLongestDuration() {
        Set<TramStations.Pair> combinations = createJourneyPairs(TramStations.EndOfTheLine, TramStations.EndOfTheLine);

        List<Journey> allResults = new ArrayList<>();

        Map<TramStations.Pair, JourneyOrNot> results = validateAllHaveAtLeastOneJourney(when,
                combinations, TramTime.of(9,0));
        results.forEach((route, journey) -> journey.ifPresent(allResults::add));

        double longest = allResults.stream().map(RouteCalculatorTest::costOfJourney).max(Integer::compare).get();
        assertEquals(testConfig.getMaxJourneyDuration(), longest, 0.001);

    }

    private void checkRouteNextNDays(Set<TramStations.Pair> combinations, LocalDate date, TramTime time) {
        for(int day = 0; day< TestEnv.DAYS_AHEAD; day++) {
            LocalDate testDate = avoidChristmasDate(date.plusDays(day));
            validateAllHaveAtLeastOneJourney(testDate, combinations, time);
        }
    }

    private Map<TramStations.Pair, JourneyOrNot> validateAllHaveAtLeastOneJourney(
            final LocalDate queryDate, final Set<TramStations.Pair> combinations, final TramTime queryTime) {

        // check each pair, collect results into (station,station)->result
        Map<TramStations.Pair, JourneyOrNot> results = computeJourneys(queryDate, combinations, queryTime, false);

        assertEquals(combinations.size(), results.size());
        // check all results present, collect failures into a list
        List<JourneyOrNot> failed = results.values().stream().
                filter(JourneyOrNot::missing).
                collect(Collectors.toList());

        assertEquals(Collections.emptyList(), failed);
        return results;
    }

    @NotNull
    private Map<TramStations.Pair, JourneyOrNot> computeJourneys(LocalDate queryDate, Set<TramStations.Pair> combinations,
                                                                      TramTime queryTime, boolean diag) {
        return combinations.parallelStream().
                map(requested -> {
                    try (Transaction txn = database.beginTx()) {
                        Optional<Journey> optionalJourney = calculator.calculateRoute(txn,
                                TramStations.real(stationRepository, requested.getStart()),
                                TramStations.real(stationRepository, requested.getDest()),
                                new JourneyRequest(new TramServiceDate(queryDate), queryTime, false, 3,
                                        testConfig.getMaxJourneyDuration()).setDiag(diag)).
                                limit(1).findAny();

                        JourneyOrNot journeyOrNot = new JourneyOrNot(requested, queryDate, queryTime, optionalJourney);
                        return Pair.of(requested, journeyOrNot);
                    }
                }).
                collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
    }

    private Set<TramStations.Pair> createJourneyPairs(List<TramStations> starts, List<TramStations> ends) {
        Set<TramStations.Pair> combinations = new HashSet<>();
        for (TramStations start : starts) {
            for (TramStations dest : ends) {
                if (!dest.equals(start)) {
                    combinations.add(new TramStations.Pair(start, dest));
                }
            }
        }
        return combinations;
    }

    private static class JourneyOrNot {
        private final TramStations.Pair requested;
        private final LocalDate queryDate;
        private final TramTime queryTime;
        private final Journey journey;

        public JourneyOrNot(TramStations.Pair requested, LocalDate queryDate, TramTime queryTime,
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
                    ", from=" + requested.getStart().getName() +
                    ", to=" + requested.getDest().getName() +
                    '}';
        }
    }

}

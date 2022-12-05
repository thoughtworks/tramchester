package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.StationIdPair;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.Durations;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.integration.testSupport.RouteCalculationCombinations;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.DataExpiryCategory;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.time.DayOfWeek;
import java.time.Duration;
import java.util.*;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.domain.reference.TransportMode.TramsOnly;
import static com.tramchester.testSupport.TestEnv.avoidChristmasDate;
import static com.tramchester.testSupport.reference.TramStations.Ashton;
import static com.tramchester.testSupport.reference.TramStations.ShawAndCrompton;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("JUnitTestMethodWithNoAssertions")
class RouteCalculatorKeyRoutesTest {

    private static ComponentContainer componentContainer;
    private static TramchesterConfig testConfig;

    private TramDate when;
    private RouteCalculationCombinations combinations;
    private JourneyRequest journeyRequest;
    private Duration maxJourneyDuration;
    private Set<TransportMode> modes;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        testConfig = new IntegrationTramTestConfig();

        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        when = TestEnv.testDay();
        modes = TramsOnly;
        maxJourneyDuration = Duration.ofMinutes(testConfig.getMaxJourneyDuration());
        int maxChanges = 4;
        journeyRequest = new JourneyRequest(when, TramTime.of(8, 5), false, maxChanges,
                maxJourneyDuration, 1, modes);
        combinations = new RouteCalculationCombinations(componentContainer);
    }

    @Test
    void shouldFindEndOfRoutesToInterchanges() {
        Set<StationIdPair> stationIdPairs = combinations.EndOfRoutesToInterchanges(Tram);
        Map<StationIdPair, RouteCalculationCombinations.JourneyOrNot> results = combinations.getJourneysFor(stationIdPairs, journeyRequest);
        validateFor(results);
    }


    @Test
    void shouldFindEndOfRoutesToEndOfRoute() {
        Set<StationIdPair> stationIdPairs = combinations.EndOfRoutesToEndOfRoutes(Tram);
        Map<StationIdPair, RouteCalculationCombinations.JourneyOrNot> results = combinations.getJourneysFor(stationIdPairs, journeyRequest);
        validateFor(results);
    }

    @Test
    void shouldFindInterchangesToEndOfRoutes() {
        Set<StationIdPair> stationIdPairs = combinations.InterchangeToEndRoutes(Tram);
        Map<StationIdPair, RouteCalculationCombinations.JourneyOrNot> results = combinations.getJourneysFor(stationIdPairs, journeyRequest);
        validateFor(results);
    }

    @Test
    void shouldFindInterchangesToInterchanges() {
        Set<StationIdPair> stationIdPairs = combinations.InterchangeToInterchange(Tram);
        Map<StationIdPair, RouteCalculationCombinations.JourneyOrNot> results = combinations.getJourneysFor(stationIdPairs, journeyRequest);
        validateFor(results);
    }

    @DataExpiryCategory
    @Test
    @Disabled("issue with data, some route appear to be missing at this time")
    void shouldFindEndOfLinesToEndOfLinesNextNDays() {

        // TODO Issue with exchange square on Sundays in current data

        final Set<StationIdPair> pairs = combinations.EndOfRoutesToEndOfRoutes(Tram);

        final Map<TramDate, Set<StationIdPair>> missing = new HashMap<>();

        for(int day = 0; day< TestEnv.DAYS_AHEAD; day++) {
            TramDate testDate = avoidChristmasDate(when.plusDays(day));
            if (testDate.getDayOfWeek() != DayOfWeek.SUNDAY) {
                JourneyRequest request = new JourneyRequest(testDate, TramTime.of(8, 5), false, 4,
                        maxJourneyDuration, 1, modes);
                Map<StationIdPair, RouteCalculationCombinations.JourneyOrNot> results = combinations.getJourneysFor(pairs, request);
                Set<StationIdPair> missingForDate = combinations.getMissing(results);
                if (!missingForDate.isEmpty()) {
                    missing.put(testDate, missingForDate);
                }
            }
        }

        assertTrue(missing.isEmpty(), missing.toString());

    }

    @DataExpiryCategory
    @Test
    void shouldFindEndOfLinesToEndOfLinesInNDays() {
        final Set<StationIdPair> pairs = combinations.EndOfRoutesToEndOfRoutes(Tram);
        // helps with diagnosis when trams not running on a specific day vs. actual missing data

        TramDate testDate = avoidChristmasDate(when);
        JourneyRequest request = new JourneyRequest(testDate, TramTime.of(8,5), false, 4,
                maxJourneyDuration, 1, modes);
        Map<StationIdPair, RouteCalculationCombinations.JourneyOrNot> results = combinations.getJourneysFor(pairs, request);
        validateFor(results);
    }

    @Test
    void shouldFindEndOfLinesToEndOfLinesFindLongestDuration() {
        List<Journey> allResults = new ArrayList<>();

        JourneyRequest longestJourneyRequest = new JourneyRequest(when, TramTime.of(9, 0), false, 4,
                maxJourneyDuration.multipliedBy(2), 1, modes);

        Map<StationIdPair, RouteCalculationCombinations.JourneyOrNot> results =
                combinations.getJourneysFor(combinations.EndOfRoutesToEndOfRoutes(Tram), longestJourneyRequest);

        validateFor(results);

        results.forEach((route, journey) -> journey.ifPresent(allResults::add));

        final Optional<Duration> max = allResults.stream().map(RouteCalculatorTest::costOfJourney).max(Duration::compareTo);
        assertTrue(max.isPresent());
        Duration longest = max.get();

        int maxJourneyDuration = testConfig.getMaxJourneyDuration();
        assertTrue(Durations.greaterOrEquals(Duration.ofMinutes(maxJourneyDuration), longest), "longest was " + longest + " and not " + maxJourneyDuration);
    }
    
    @Disabled("used for diagnosing specific issue")
    @Test
    void shouldRepoServiceTimeIssueForConcurrency() {
        GraphDatabase database = componentContainer.get(GraphDatabase.class);

        List<StationIdPair> stationIdPairs = new ArrayList<>();
        for (int i = 0; i < 99; i++) {
            stationIdPairs.add(new StationIdPair(ShawAndCrompton, Ashton));
        }

        TramDate queryDate = when;
        TramTime queryTime = TramTime.of(8,0);

        Optional<Pair<StationIdPair, RouteCalculationCombinations.JourneyOrNot>> failed = stationIdPairs.parallelStream().
                map(requested -> {
                    try (Transaction txn = database.beginTx()) {
                        JourneyRequest journeyRequest = new JourneyRequest(queryDate, queryTime, false,
                                3, maxJourneyDuration, 1, modes);
                        Optional<Journey> optionalJourney = combinations.findJourneys(txn, requested.getBeginId(), requested.getEndId(),
                                journeyRequest);
                        RouteCalculationCombinations.JourneyOrNot journeyOrNot =
                                new RouteCalculationCombinations.JourneyOrNot(requested, queryDate, queryTime, optionalJourney);
                        return Pair.of(requested, journeyOrNot);
                    }
                }).filter(pair -> pair.getRight().missing()).findAny();

        assertFalse(failed.isPresent());
    }

    private void validateFor(Map<StationIdPair, RouteCalculationCombinations.JourneyOrNot> results) {
        Set<StationIdPair> missingForDate = combinations.getMissing(results);
        assertTrue(missingForDate.isEmpty(), missingForDate.toString());
    }

}

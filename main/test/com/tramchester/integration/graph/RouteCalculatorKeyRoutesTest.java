package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.StationIdPair;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.dates.TramServiceDate;
import com.tramchester.domain.time.Durations;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.integration.testSupport.IntegrationTestConfig;
import com.tramchester.integration.testSupport.RouteCalculationCombinations;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.DataExpiryCategory;
import com.tramchester.testSupport.testTags.PiccGardens2022;
import com.tramchester.testSupport.testTags.VictoriaNov2022;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.time.Duration;
import java.util.*;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.integration.testSupport.IntegrationTestConfig.VictoriaClosureDate;
import static com.tramchester.testSupport.TestEnv.avoidChristmasDate;
import static com.tramchester.testSupport.reference.TramStations.Ashton;
import static com.tramchester.testSupport.reference.TramStations.ShawAndCrompton;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("JUnitTestMethodWithNoAssertions")
class RouteCalculatorKeyRoutesTest {

    private static ComponentContainer componentContainer;
    private static TramchesterConfig testConfig;

    private TramDate when;
    private RouteCalculationCombinations combinations;
    private JourneyRequest journeyRequest;
    private Duration maxJourneyDuration;

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
        when = TestEnv.testTramDay();
        maxJourneyDuration = Duration.ofMinutes(testConfig.getMaxJourneyDuration());
        int maxChanges = 4;
        journeyRequest = new JourneyRequest(when, TramTime.of(8, 5), false, maxChanges,
                maxJourneyDuration, 1, Collections.emptySet());
        combinations = new RouteCalculationCombinations(componentContainer);
    }

    @Test
    void shouldFindEndOfRoutesToInterchanges() {
        combinations.validateAllHaveAtLeastOneJourney(combinations.EndOfRoutesToInterchanges(Tram), journeyRequest);
    }

    @Test
    void shouldFindEndOfRoutesToEndOfRoute() {
        combinations.validateAllHaveAtLeastOneJourney(combinations.EndOfRoutesToEndOfRoutes(Tram), journeyRequest);
    }

    @Test
    void shouldFindInterchangesToEndOfRoutes() {
        combinations.validateAllHaveAtLeastOneJourney(combinations.InterchangeToEndRoutes(Tram), journeyRequest);
    }

    @Test
    void shouldFindInterchangesToInterchanges() {
        combinations.validateAllHaveAtLeastOneJourney(combinations.InterchangeToInterchange(Tram), journeyRequest);
    }

    @VictoriaNov2022
    @DataExpiryCategory
    @Test
    void shouldFindEndOfLinesToEndOfLinesNextNDays() {


        final Set<StationIdPair> pairs = combinations.EndOfRoutesToEndOfRoutes(Tram);

        for(int day = 0; day< TestEnv.DAYS_AHEAD; day++) {
            TramDate testDate = avoidChristmasDate(when.plusDays(day));
            if (!VictoriaClosureDate.equals(testDate)) {
                JourneyRequest request = new JourneyRequest(testDate, TramTime.of(8, 5), false, 4,
                        maxJourneyDuration, 1, Collections.emptySet());
                combinations.validateAllHaveAtLeastOneJourney(pairs, request);
            }
        }
    }

    @DataExpiryCategory
    @Test
    void shouldFindEndOfLinesToEndOfLinesInNDays() {
        final Set<StationIdPair> pairs = combinations.EndOfRoutesToEndOfRoutes(Tram);
        // helps with diagnosis when trams not running on a specific day vs. actual missing data

        TramDate testDate = avoidChristmasDate(when.plusDays(TestEnv.DAYS_AHEAD));
        JourneyRequest request = new JourneyRequest(testDate, TramTime.of(8,5), false, 4,
                maxJourneyDuration, 1, Collections.emptySet());
        combinations.validateAllHaveAtLeastOneJourney(pairs, request);

    }

    @Test
    void shouldFindEndOfLinesToEndOfLinesFindLongestDuration() {
        List<Journey> allResults = new ArrayList<>();

        JourneyRequest longestJourneyRequest = new JourneyRequest(when, TramTime.of(9, 0), false, 4,
                maxJourneyDuration.multipliedBy(2), 1, Collections.emptySet());

        Map<StationIdPair, RouteCalculationCombinations.JourneyOrNot> results =
                combinations.validateAllHaveAtLeastOneJourney(combinations.EndOfRoutesToEndOfRoutes(Tram), longestJourneyRequest);
        results.forEach((route, journey) -> journey.ifPresent(allResults::add));

        // allResults.stream().map(RouteCalculatorTest::costOfJourney).max(Integer::compare);
        final Optional<Duration> max = allResults.stream().map(RouteCalculatorTest::costOfJourney).max(Duration::compareTo);
        assertTrue(max.isPresent());
        Duration longest = max.get();
        assertTrue(Durations.greaterOrEquals(Duration.ofMinutes(testConfig.getMaxJourneyDuration()),longest), "longest was " + longest);
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
                        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(queryDate), queryTime, false,
                                3, maxJourneyDuration, 1, Collections.emptySet());
                        Optional<Journey> optionalJourney = combinations.findJourneys(txn, requested.getBeginId(), requested.getEndId(),
                                journeyRequest);
                        RouteCalculationCombinations.JourneyOrNot journeyOrNot =
                                new RouteCalculationCombinations.JourneyOrNot(requested, queryDate, queryTime, optionalJourney);
                        return Pair.of(requested, journeyOrNot);
                    }
                }).filter(pair -> pair.getRight().missing()).findAny();

        assertFalse(failed.isPresent());
    }

}

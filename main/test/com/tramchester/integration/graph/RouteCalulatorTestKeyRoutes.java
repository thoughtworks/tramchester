package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.integration.graph.testSupport.RouteCalculationCombinations;
import com.tramchester.integration.testSupport.IntegrationTramTestConfig;
import com.tramchester.testSupport.DataExpiryCategory;
import com.tramchester.testSupport.StationPair;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.testSupport.TestEnv.avoidChristmasDate;
import static com.tramchester.testSupport.reference.TramStations.Ashton;
import static com.tramchester.testSupport.reference.TramStations.ShawAndCrompton;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SuppressWarnings("JUnitTestMethodWithNoAssertions")
class RouteCalulatorTestKeyRoutes {

    private static ComponentContainer componentContainer;
    private static TramchesterConfig testConfig;
    private static GraphDatabase database;

    private final LocalDate when = TestEnv.testDay();
    private RouteCalculationCombinations combinations;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        testConfig = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder<>().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        combinations = new RouteCalculationCombinations(componentContainer, testConfig);
    }


    @Disabled("used for diagnosing specific issue")
    @Test
    void shouldRepoServiceTimeIssueForConcurrency() {
        List<StationPair> stationPairs = new ArrayList<>();
        for (int i = 0; i < 99; i++) {
            stationPairs.add(new StationPair(ShawAndCrompton, Ashton));
        }

        LocalDate queryDate = when;
        TramTime queryTime = TramTime.of(8,0);

        Optional<Pair<StationPair, RouteCalculationCombinations.JourneyOrNot>> failed = stationPairs.parallelStream().
                map(requested -> {
                    try (Transaction txn = database.beginTx()) {
                        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(queryDate), queryTime, false,
                                3, testConfig.getMaxJourneyDuration());
                        //journeyRequest.setDiag(diag);
                        Optional<Journey> optionalJourney = combinations.findJourneys(txn, requested.getBegin(), requested.getEnd(), journeyRequest);
                        RouteCalculationCombinations.JourneyOrNot journeyOrNot = new RouteCalculationCombinations.JourneyOrNot(requested, queryDate, queryTime, optionalJourney);
                        return Pair.of(requested, journeyOrNot);
                    }
                }).filter(pair -> pair.getRight().missing()).findAny();

        assertFalse(failed.isPresent());
    }

    @Test
    void shouldFindEndOfRoutesToInterchanges() {
        validateAllHaveAtLeastOneJourney(when, combinations.EndOfRoutesToInterchanges(Tram), TramTime.of(8,0));
    }

    @Test
    void shouldFindEndOfRoutesToEndOfRoute() {
        validateAllHaveAtLeastOneJourney(when, combinations.EndOfRoutesToEndOfRoutes(Tram), TramTime.of(8,0));
    }

    @Test
    void shouldFindInterchangesToEndOfRoutes() {
        validateAllHaveAtLeastOneJourney(when, combinations.InterchangeToEndRoutes(Tram), TramTime.of(8,0));
    }

    @Test
    void shouldFindInterchangesToInterchanges() {
        validateAllHaveAtLeastOneJourney(when, combinations.InterchangeToInterchange(Tram), TramTime.of(8,0));
    }

    @DataExpiryCategory
    @Test
    void shouldFindEndOfLinesToEndOfLinesNextNDays() {
        // todo: lockdown, changed from 9 to 10.15 as airport to eccles fails for 10.15am
        checkRouteNextNDays(combinations.EndOfRoutesToEndOfRoutes(Tram), when, TramTime.of(10,15));
    }

    @Test
    void shouldFindEndOfLinesToEndOfLinesFindLongestDuration() {
        List<Journey> allResults = new ArrayList<>();

        Map<StationPair, RouteCalculationCombinations.JourneyOrNot> results = validateAllHaveAtLeastOneJourney(when,
                combinations.EndOfRoutesToEndOfRoutes(Tram), TramTime.of(9,0));
        results.forEach((route, journey) -> journey.ifPresent(allResults::add));

        double longest = allResults.stream().map(RouteCalculatorTest::costOfJourney).max(Integer::compare).get();
        assertEquals(testConfig.getMaxJourneyDuration(), longest, 0.001);

    }

    private void checkRouteNextNDays(Set<StationPair> combinations, LocalDate date, TramTime time) {
        for(int day = 0; day< TestEnv.DAYS_AHEAD; day++) {
            LocalDate testDate = avoidChristmasDate(date.plusDays(day));
            validateAllHaveAtLeastOneJourney(testDate, combinations, time);
        }
    }

    private Map<StationPair, RouteCalculationCombinations.JourneyOrNot> validateAllHaveAtLeastOneJourney(
            final LocalDate queryDate, final Set<StationPair> stationPairs, final TramTime queryTime) {

        // check each pair, collect results into (station,station)->result
        Map<StationPair, RouteCalculationCombinations.JourneyOrNot> results = combinations.computeJourneys(queryDate, stationPairs, queryTime);

        assertEquals(stationPairs.size(), results.size());
        // check all results present, collect failures into a list
        List<RouteCalculationCombinations.JourneyOrNot> failed = results.values().stream().
                filter(RouteCalculationCombinations.JourneyOrNot::missing).
                collect(Collectors.toList());

        assertEquals(Collections.emptyList(), failed);
        return results;
    }

    @Deprecated
    private Set<StationPair> createJourneyPairs(Set<TramStations> starts, Set<TramStations> ends) {
        Set<StationPair> combinations = new HashSet<>();
        for (TramStations start : starts) {
            for (TramStations dest : ends) {
                if (!dest.equals(start)) {
                    combinations.add(new StationPair(start, dest));
                }
            }
        }
        return combinations;
    }

}

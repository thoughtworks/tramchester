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
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.testSupport.DataExpiryCategory;
import com.tramchester.domain.StationIdPair;
import com.tramchester.testSupport.TestEnv;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.*;

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

    @Test
    void shouldFindEndOfRoutesToInterchanges() {
        combinations.validateAllHaveAtLeastOneJourney(when, combinations.EndOfRoutesToInterchanges(Tram), TramTime.of(8,0));
    }

    @Test
    void shouldFindEndOfRoutesToEndOfRoute() {
        combinations.validateAllHaveAtLeastOneJourney(when, combinations.EndOfRoutesToEndOfRoutes(Tram), TramTime.of(8,0));
    }

    @Test
    void shouldFindInterchangesToEndOfRoutes() {
        combinations.validateAllHaveAtLeastOneJourney(when, combinations.InterchangeToEndRoutes(Tram), TramTime.of(8,0));
    }

    @Test
    void shouldFindInterchangesToInterchanges() {
        combinations.validateAllHaveAtLeastOneJourney(when, combinations.InterchangeToInterchange(Tram), TramTime.of(8,0));
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

        Map<StationIdPair, RouteCalculationCombinations.JourneyOrNot> results = combinations.validateAllHaveAtLeastOneJourney(when,
                combinations.EndOfRoutesToEndOfRoutes(Tram), TramTime.of(9,0));
        results.forEach((route, journey) -> journey.ifPresent(allResults::add));

        double longest = allResults.stream().map(RouteCalculatorTest::costOfJourney).max(Integer::compare).get();
        assertEquals(testConfig.getMaxJourneyDuration(), longest, 0.001);

    }

    @Disabled("used for diagnosing specific issue")
    @Test
    void shouldRepoServiceTimeIssueForConcurrency() {
        GraphDatabase database = componentContainer.get(GraphDatabase.class);

        List<StationIdPair> stationIdPairs = new ArrayList<>();
        for (int i = 0; i < 99; i++) {
            stationIdPairs.add(new StationIdPair(ShawAndCrompton, Ashton));
        }

        LocalDate queryDate = when;
        TramTime queryTime = TramTime.of(8,0);

        Optional<Pair<StationIdPair, RouteCalculationCombinations.JourneyOrNot>> failed = stationIdPairs.parallelStream().
                map(requested -> {
                    try (Transaction txn = database.beginTx()) {
                        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(queryDate), queryTime, false,
                                3, testConfig.getMaxJourneyDuration());
                        Optional<Journey> optionalJourney = combinations.findJourneys(txn, requested.getBeginId(), requested.getEndId(), journeyRequest);
                        RouteCalculationCombinations.JourneyOrNot journeyOrNot = new RouteCalculationCombinations.JourneyOrNot(requested, queryDate, queryTime, optionalJourney);
                        return Pair.of(requested, journeyOrNot);
                    }
                }).filter(pair -> pair.getRight().missing()).findAny();

        assertFalse(failed.isPresent());
    }

    private void checkRouteNextNDays(Set<StationIdPair> stationIdPairs, LocalDate date, TramTime time) {
        for(int day = 0; day< TestEnv.DAYS_AHEAD; day++) {
            LocalDate testDate = avoidChristmasDate(date.plusDays(day));
            combinations.validateAllHaveAtLeastOneJourney(testDate, stationIdPairs, time);
        }
    }

}

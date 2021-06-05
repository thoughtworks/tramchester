package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.integration.graph.testSupport.RouteCalculationCombinations;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.TransportData;
import com.tramchester.domain.StationIdPair;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Tram;

class RouteCalculatorTestAllJourneys {

    private static ComponentContainer componentContainer;
    private static IntegrationTramTestConfig testConfig;

    private final LocalDate when = TestEnv.testDay();
    private RouteCalculationCombinations combinations;

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
        combinations = new RouteCalculationCombinations(componentContainer);
    }

    @Test
    void shouldFindRouteEachStationToEveryOtherStream() {
        TransportData data = componentContainer.get(TransportData.class);

        Set<Station> allStations = data.getStationsForMode(Tram);

        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8, 5), false, 2,
                testConfig.getMaxJourneyDuration(), 1);

        // pairs of stations to check
        Set<StationIdPair> stationIdPairs = allStations.stream().flatMap(start -> allStations.stream().
                filter(dest -> !combinations.betweenInterchanges(start, dest)).
                map(dest -> StationIdPair.of(start, dest))).
                filter(pair -> !pair.same()).
                filter(pair -> !combinations.betweenEndsOfRoute(pair)).
                collect(Collectors.toSet());

        Map<StationIdPair, RouteCalculationCombinations.JourneyOrNot> results =
                combinations.validateAllHaveAtLeastOneJourney(stationIdPairs, journeyRequest);

        // now find longest journey
        Optional<Integer> maxNumberStops = results.values().stream().
                filter(journeyOrNot -> !journeyOrNot.missing()).
                map(RouteCalculationCombinations.JourneyOrNot::get).
                map(journey -> journey.getStages().stream().
                        map(TransportStage::getPassedStopsCount).
                        reduce(Integer::sum)).
                filter(Optional::isPresent).
                map(Optional::get).
                max(Integer::compare);

        Assertions.assertTrue(maxNumberStops.isPresent());
        Assertions.assertEquals(39, maxNumberStops.get().intValue());
    }

}

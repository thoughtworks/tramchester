package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.StationIdPair;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.RouteCalculationCombinations;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.Summer2022;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Tram;

class RouteCalculatorTestAllJourneys {

    private static ComponentContainer componentContainer;
    private static IntegrationTramTestConfig testConfig;

    private TramDate when;
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
        when = TestEnv.testDay();
        combinations = new RouteCalculationCombinations(componentContainer);
    }

    @Test
    void shouldFindRouteEachStationToEveryOtherStream() {
        TransportData data = componentContainer.get(TransportData.class);

        final TramTime time = TramTime.of(8, 5);
        Set<Station> haveServices = new HashSet<>(data.getStationsServing(Tram));

        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, 2,
                Duration.ofMinutes(testConfig.getMaxJourneyDuration()), 1, Collections.emptySet());

        // pairs of stations to check
        Set<StationIdPair> stationIdPairs = haveServices.stream().flatMap(start -> haveServices.stream().
                filter(dest -> !combinations.betweenInterchanges(start, dest)).
                map(dest -> StationIdPair.of(start, dest))).
                filter(pair -> !pair.same()).
                // was here to avoid duplication....
                //filter(pair -> !combinations.betweenEndsOfRoute(pair)).
                collect(Collectors.toSet());

        combinations.validateAllHaveAtLeastOneJourney(stationIdPairs, journeyRequest);

    }


}

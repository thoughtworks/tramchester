package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Journey;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.graph.testSupport.RouteCalculationCombinations;
import com.tramchester.integration.testSupport.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.StationPair;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

class RouteCalculatorTestAllJourneys {

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;

    private static IntegrationTramTestConfig testConfig;

    private final LocalDate when = TestEnv.testDay();
    private RouteCalculationCombinations routeCalculationCombinations;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        testConfig = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder<>().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        database = componentContainer.get(GraphDatabase.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        RouteCalculator calculator = componentContainer.get(RouteCalculator.class);
        StationRepository stationRepository = componentContainer.get(StationRepository.class);
        routeCalculationCombinations = new RouteCalculationCombinations(database, calculator, stationRepository, testConfig);
    }

    @Test
    void shouldFindRouteEachStationToEveryOtherStream() {
        TransportData data = componentContainer.get(TransportData.class);

        Set<Station> allStations = data.getStations();

        // pairs of stations to check
        Set<StationPair> combinations = allStations.stream().flatMap(start -> allStations.stream().
                map(dest -> StationPair.of(start, dest))).
                filter(pair -> !pair.same()).
                filter(pair -> !interchanes(pair)).
                filter(pair -> !endsOfLine(pair)).
                collect(Collectors.toSet());

        Map<StationPair, Optional<Journey>> results = routeCalculationCombinations.validateAllHaveAtLeastOneJourney(when,
                combinations, TramTime.of(8, 5));

        // now find longest journey
        Optional<Integer> maxNumberStops = results.values().stream().
                filter(Optional::isPresent).
                map(Optional::get).
                map(journey -> journey.getStages().stream().
                        map(TransportStage::getPassedStopsCount).
                        reduce(Integer::sum)).
                filter(Optional::isPresent).
                map(Optional::get).
                max(Integer::compare);

        Assertions.assertTrue(maxNumberStops.isPresent());
        Assertions.assertEquals(39, maxNumberStops.get().intValue());
    }

    private boolean endsOfLine(StationPair pair) {
        return TramStations.isEndOfLine(pair.getBegin()) && TramStations.isEndOfLine(pair.getEnd());
    }

    private boolean interchanes(StationPair pair) {
        return TramStations.isInterchange(pair.getBegin()) && TramStations.isInterchange(pair.getEnd());
    }


}

package com.tramchester.integration.graph.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.filters.ConfigurableGraphFilter;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.integration.testSupport.RouteCalculationCombinations;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.testTags.BusTest;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;

import static com.tramchester.domain.reference.TransportMode.Bus;

@SuppressWarnings("JUnitTestMethodWithNoAssertions")
@BusTest
@Disabled("takes too long for this many of stations")
class RouteCalculatorKeyRoutesBusTest {

    private static ComponentContainer componentContainer;
    private static TramchesterConfig testConfig;

    private final TramDate when = TestEnv.testDay();
    private RouteCalculationCombinations combinations;
    private JourneyRequest journeyRequest;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {
        testConfig = new IntegrationBusTestConfig("warringtonsOwnBuses.db");
        TestEnv.deleteDBIfPresent(testConfig);
        componentContainer = new ComponentsBuilder().
                configureGraphFilter(RouteCalculatorKeyRoutesBusTest::configureFilter).
                create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    static void configureFilter(ConfigurableGraphFilter graphFilter, TransportData transportData) {
        graphFilter.addAgency(TestEnv.WarringtonsOwnBuses.getId());
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() throws IOException {
        componentContainer.close();
        TestEnv.deleteDBIfPresent(testConfig);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        combinations = new RouteCalculationCombinations(componentContainer);
        TramTime time = TramTime.of(8, 0);
        int numberChanges = 3;
        journeyRequest = new JourneyRequest(when, time, false, numberChanges,
                Duration.ofMinutes(testConfig.getMaxJourneyDuration()), 1, Collections.emptySet());
    }

    @Test
    void shouldFindEndOfRoutesToInterchanges() {
        combinations.validateAllHaveAtLeastOneJourney(combinations.EndOfRoutesToInterchanges(Bus), journeyRequest);
    }

    @Test
    void shouldFindEndOfRoutesToEndOfRoute() {
        combinations.validateAllHaveAtLeastOneJourney(combinations.EndOfRoutesToEndOfRoutes(Bus), journeyRequest);
    }

    @Test
    void shouldFindInterchangesToEndOfRoutes() {
        combinations.validateAllHaveAtLeastOneJourney(combinations.InterchangeToEndRoutes(Bus), journeyRequest);
    }

    @Test
    void shouldFindInterchangesToInterchanges() {
        combinations.validateAllHaveAtLeastOneJourney(combinations.InterchangeToInterchange(Bus), journeyRequest);
    }
}

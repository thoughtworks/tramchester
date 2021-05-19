package com.tramchester.integration.graph.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.filters.ConfigurableGraphFilter;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.integration.graph.testSupport.RouteCalculationCombinations;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.time.LocalDate;

import static com.tramchester.domain.reference.TransportMode.Bus;

@SuppressWarnings("JUnitTestMethodWithNoAssertions")
@Disabled("takes too long for thousands of stations")
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class RouteCalculatorKeyRoutesBusTest {

    private static ComponentContainer componentContainer;
    private static TramchesterConfig testConfig;

    private final LocalDate when = TestEnv.testDay();
    private RouteCalculationCombinations combinations;
    private TramTime time;
    private JourneyRequest journeyRequest;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        testConfig = new IntegrationBusTestConfig();
        componentContainer = new ComponentsBuilder().
                configureGraphFilter(RouteCalculatorKeyRoutesBusTest::configureFilter).
                create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    static void configureFilter(ConfigurableGraphFilter graphFilter) {
        graphFilter.addAgency(TestEnv.WarringtonsOwnBuses.getId());
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        combinations = new RouteCalculationCombinations(componentContainer);
        time = TramTime.of(8, 0);
        int numberChanges = 3;
        journeyRequest = new JourneyRequest(when, time, false, numberChanges, testConfig.getMaxJourneyDuration());
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

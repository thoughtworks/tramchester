package com.tramchester.integration.graph.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.graph.testSupport.RouteCalculationCombinations;
import com.tramchester.integration.testSupport.IntegrationBusTestConfig;
import com.tramchester.integration.testSupport.IntegrationTramTestConfig;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.tramchester.domain.reference.TransportMode.Bus;

@SuppressWarnings("JUnitTestMethodWithNoAssertions")
class RouteCalculatorKeyRoutesBusTest {

    private static ComponentContainer componentContainer;
    private static TramchesterConfig testConfig;

    private final LocalDate when = TestEnv.testDay();
    private RouteCalculationCombinations combinations;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        testConfig = new IntegrationBusTestConfig();
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
        combinations.validateAllHaveAtLeastOneJourney(when, combinations.EndOfRoutesToInterchanges(Bus), TramTime.of(8,0));
    }

    @Test
    void shouldFindEndOfRoutesToEndOfRoute() {
        combinations.validateAllHaveAtLeastOneJourney(when, combinations.EndOfRoutesToEndOfRoutes(Bus), TramTime.of(8,0));
    }

    @Test
    void shouldFindInterchangesToEndOfRoutes() {
        combinations.validateAllHaveAtLeastOneJourney(when, combinations.InterchangeToEndRoutes(Bus), TramTime.of(8,0));
    }

    @Test
    void shouldFindInterchangesToInterchanges() {
        combinations.validateAllHaveAtLeastOneJourney(when, combinations.InterchangeToInterchange(Bus), TramTime.of(8,0));
    }
}

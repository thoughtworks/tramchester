package com.tramchester.integration.graph.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.graph.filters.ConfigurableGraphFilter;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.graph.graphbuild.StationsAndLinksGraphBuilder;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.io.IOException;

@SuppressWarnings("JUnitTestMethodWithNoAssertions")
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class GraphBuildForBusPerformanceTest {

    private static ComponentContainer componentContainer;
    private static IntegrationBusTestConfig testConfig;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {
        testConfig = new IntegrationBusTestConfig("GraphBuildForBusPerformanceTest.db");
        TestEnv.deleteDBIfPresent(testConfig);

        componentContainer = new ComponentsBuilder().
                configureGraphFilter(GraphBuildForBusPerformanceTest::configureFilter).
                create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    private static void configureFilter(ConfigurableGraphFilter graphFilter) {
        graphFilter.addAgency(TestEnv.WarringtonsOwnBuses.getId());
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() throws IOException {
        componentContainer.close();
        TestEnv.deleteDBIfPresent(testConfig);
    }

    @Test
    void shouldTriggerMainGraphBuild() {
        componentContainer.get(StagedTransportGraphBuilder.Ready.class);
    }

    @Test
    void shouldTriggerLinksBuild() {
        componentContainer.get(StationsAndLinksGraphBuilder.Ready.class);
    }
}



package com.tramchester.integration.graph.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.graph.graphbuild.ActiveGraphFilter;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.integration.testSupport.IntegrationBusTestConfig;
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

        ActiveGraphFilter graphFilter = new ActiveGraphFilter();
        graphFilter.addAgency(TestEnv.StagecoachManchester.getId());

        componentContainer = new ComponentsBuilder<>().setGraphFilter(graphFilter).create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() throws IOException {
        componentContainer.close();
        TestEnv.deleteDBIfPresent(testConfig);
    }

    @Test
    void shouldTriggerBuild() {
        StagedTransportGraphBuilder.Ready ready = componentContainer.get(StagedTransportGraphBuilder.Ready.class);
    }
}



package com.tramchester.integration.graph.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.graph.graphbuild.ActiveGraphFilter;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.integration.testSupport.IntegrationBusTestConfig;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

@SuppressWarnings("JUnitTestMethodWithNoAssertions")
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class GraphBuildForBusPerformanceTest {

    private static ComponentContainer componentContainer;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        ActiveGraphFilter graphFilter = new ActiveGraphFilter();
        graphFilter.addAgency(StringIdFor.createId("GMS"));

        TramchesterConfig testConfig = new IntegrationBusTestConfig();
        componentContainer = new ComponentsBuilder<>().setGraphFilter(graphFilter).create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @Test
    void shouldTriggerBuild() {
        StagedTransportGraphBuilder.Ready ready = componentContainer.get(StagedTransportGraphBuilder.Ready.class);
    }
}



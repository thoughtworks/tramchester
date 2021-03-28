package com.tramchester.unit.graph;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.graph.NumberOfNodesRepository;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramTransportDataForTestFactory;
import com.tramchester.unit.graph.calculation.SimpleGraphConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NumberOfNodesRepositoryTest {

    private static SimpleGraphConfig config;
    private static GuiceContainerDependencies<TramTransportDataForTestFactory> componentContainer;
    private NumberOfNodesRepository repository;

    @BeforeAll
    static void onceBeforeAllTestRuns() throws IOException {
        config = new SimpleGraphConfig("ServiceNodeCacheTest.db");
        TestEnv.deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder<TramTransportDataForTestFactory>().
                overrideProvider(TramTransportDataForTestFactory.class).
                create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void onceAfterAllTestsRun() throws IOException {
        componentContainer.close();
        TestEnv.deleteDBIfPresent(config);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        repository = componentContainer.get(NumberOfNodesRepository.class);
    }

    @Test
    void shouldHaveCorrectNumberOfNodesOfType() {

        assertEquals(0, repository.numberOf(GraphBuilder.Labels.SUBWAY_STATION));
        assertEquals(0, repository.numberOf(GraphBuilder.Labels.BUS_STATION));
        assertEquals(0, repository.numberOf(GraphBuilder.Labels.QUERY_NODE));
        assertEquals(0, repository.numberOf(GraphBuilder.Labels.TRAIN_STATION));

        assertEquals(6, repository.numberOf(GraphBuilder.Labels.TRAM_STATION));
        assertEquals(6, repository.numberOf(GraphBuilder.Labels.HOUR));
        assertEquals(5, repository.numberOf(GraphBuilder.Labels.SERVICE));
        assertEquals(7, repository.numberOf(GraphBuilder.Labels.MINUTE));
        assertEquals(6, repository.numberOf(GraphBuilder.Labels.PLATFORM));
        assertEquals(8, repository.numberOf(GraphBuilder.Labels.ROUTE_STATION));
        assertEquals(6, repository.numberOf(GraphBuilder.Labels.PLATFORM));


    }
}

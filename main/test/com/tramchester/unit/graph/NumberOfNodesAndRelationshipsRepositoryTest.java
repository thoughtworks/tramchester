package com.tramchester.unit.graph;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.graph.NumberOfNodesAndRelationshipsRepository;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramTransportDataForTestFactory;
import com.tramchester.unit.graph.calculation.SimpleCompositeGraphConfig;
import com.tramchester.unit.graph.calculation.SimpleGraphConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NumberOfNodesAndRelationshipsRepositoryTest {

    private static SimpleGraphConfig config;
    private static GuiceContainerDependencies componentContainer;
    private NumberOfNodesAndRelationshipsRepository repository;

    @BeforeAll
    static void onceBeforeAllTestRuns() throws IOException {
        config = new SimpleCompositeGraphConfig("ServiceNodeCacheTest.db");
        TestEnv.deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder().
                overrideProvider(TramTransportDataForTestFactory.class).
                create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void onceAfterAllTestsRun() throws IOException {
        TestEnv.clearDataCache(componentContainer);
        componentContainer.close();
        TestEnv.deleteDBIfPresent(config);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        repository = componentContainer.get(NumberOfNodesAndRelationshipsRepository.class);
    }

    @Test
    void shouldHaveOnlyTram() {
        assertEquals(0, repository.numberOf(GraphLabel.SUBWAY));
        assertEquals(0, repository.numberOf(GraphLabel.BUS));
        assertEquals(0, repository.numberOf(GraphLabel.QUERY_NODE));
        assertEquals(0, repository.numberOf(GraphLabel.TRAIN));
        assertEquals(0, repository.numberOf(TransportRelationshipTypes.BUS_GOES_TO));
        assertEquals(0, repository.numberOf(TransportRelationshipTypes.TRAIN_GOES_TO));
        assertEquals(0, repository.numberOf(TransportRelationshipTypes.NEIGHBOUR));
        assertEquals(0, repository.numberOf(TransportRelationshipTypes.FERRY_GOES_TO));
        assertEquals(0, repository.numberOf(TransportRelationshipTypes.WALKS_FROM_STATION));
        assertEquals(0, repository.numberOf(TransportRelationshipTypes.WALKS_TO_STATION));
    }

    @Test
    void shouldHaveCorrectNumberOfNodesStationsAndPlatform() {
        assertEquals(9, repository.numberOf(GraphLabel.STATION));
        assertEquals(9, repository.numberOf(GraphLabel.PLATFORM));
        assertEquals(11, repository.numberOf(GraphLabel.ROUTE_STATION));
    }

    @Test
    void shouldHaveCorrectNumberOfRelationsBoardDepartandTravel() {

        assertEquals(8, repository.numberOf(TransportRelationshipTypes.TRAM_GOES_TO));

        assertEquals(8, repository.numberOf(TransportRelationshipTypes.BOARD));
        assertEquals(8, repository.numberOf(TransportRelationshipTypes.DEPART));

        assertEquals(3, repository.numberOf(TransportRelationshipTypes.INTERCHANGE_BOARD));
        assertEquals(3, repository.numberOf(TransportRelationshipTypes.INTERCHANGE_DEPART));

    }

    @Test
    void shouldHaveCorrectNumberService() {
        assertEquals(6, repository.numberOf(GraphLabel.SERVICE));
        assertEquals(6, repository.numberOf(TransportRelationshipTypes.TO_SERVICE));
    }

    @Test
    void shouldHaveCorrectNumberTime() {
        assertEquals(7, repository.numberOf(GraphLabel.HOUR));
        assertEquals(8, repository.numberOf(GraphLabel.MINUTE));

        assertEquals(7, repository.numberOf(TransportRelationshipTypes.TO_HOUR));
        assertEquals(8, repository.numberOf(TransportRelationshipTypes.TO_MINUTE));
    }
}

package com.tramchester.integration.graph.rail;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.graph.search.RouteToRouteCosts;
import com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.TrainTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.neo4j.graphdb.Transaction;

import static com.tramchester.integration.testSupport.rail.RailStationIds.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TrainTest
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
public class RouteToRouteCostsRailTest {
    private static ComponentContainer componentContainer;

    private RouteToRouteCosts routeToRouteCosts;
    private StationRepository stationRepository;
    private Transaction txn;

    @BeforeAll
    static void onceBeforeAnyTestRuns() {
        TramchesterConfig config = new IntegrationRailTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        GraphDatabase database = componentContainer.get(GraphDatabase.class);

        txn = database.beginTx();
        routeToRouteCosts = componentContainer.get(RouteToRouteCosts.class);
        stationRepository = componentContainer.get(StationRepository.class);

        // full rebuild of graph, including version node so we avoid rebuild every test run
        StagedTransportGraphBuilder stagedTransportGraphBuilder = componentContainer.get(StagedTransportGraphBuilder.class);

    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldGetNumberOfRouteHopsBetweenStockportAndManPicc() {
        Station stockport = stationRepository.getStationById(Stockport.getId());
        Station manPicc = stationRepository.getStationById(ManchesterPiccadilly.getId());

        assertEquals(0, routeToRouteCosts.getNumberOfChanges(stockport, manPicc).getMin());
    }

    @Test
    void shouldGetNumberOfRouteHopsBetweenManPiccAndLondonEustom() {
        Station manPicc = stationRepository.getStationById(ManchesterPiccadilly.getId());
        Station londonEuston = stationRepository.getStationById(LondonEuston.getId());

        assertEquals(0, routeToRouteCosts.getNumberOfChanges(manPicc, londonEuston).getMin());
    }

    @Test
    void shouldGetNumberOfRouteHopsBetweenAltrinchamAndManPicc() {
        Station altrincham = stationRepository.getStationById(Altrincham.getId());
        Station londonEuston = stationRepository.getStationById(LondonEuston.getId());

        assertEquals(1, routeToRouteCosts.getNumberOfChanges(altrincham, londonEuston).getMin());
    }
}

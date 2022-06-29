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

import java.util.Collections;

import static com.tramchester.domain.reference.TransportMode.Train;
import static com.tramchester.integration.testSupport.rail.RailStationIds.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TrainTest
public class RouteToRouteCostsRailTest {
    private static ComponentContainer componentContainer;

    private RouteToRouteCosts routeToRouteCosts;
    private StationRepository stationRepository;
    private Transaction txn;
    private Station manPicc;
    private Station stockport;
    private Station londonEuston;

    @BeforeAll
    static void onceBeforeAnyTestRuns() {
        TramchesterConfig config = new IntegrationRailTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
        TestEnv.clearDataCache(componentContainer);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        TestEnv.clearDataCache(componentContainer);
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        GraphDatabase database = componentContainer.get(GraphDatabase.class);

        txn = database.beginTx();
        routeToRouteCosts = componentContainer.get(RouteToRouteCosts.class);
        stationRepository = componentContainer.get(StationRepository.class);

        // full rebuild of graph, including version node so we avoid rebuild every test run
        componentContainer.get(StagedTransportGraphBuilder.class);

        manPicc = stationRepository.getStationById(ManchesterPiccadilly.getId());
        stockport = stationRepository.getStationById(Stockport.getId());
        londonEuston = stationRepository.getStationById(LondonEuston.getId());
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldGetNumberOfRouteHopsBetweenStockportAndManPicc() {
        assertEquals(0, routeToRouteCosts.getNumberOfChanges(stockport, manPicc, Collections.singleton(Train)).getMin());
    }

    @Test
    void shouldHaveExpectedNumberHopsChangesManToStockport() {
        assertEquals(0, routeToRouteCosts.getNumberOfChanges(manPicc, stockport, Collections.singleton(Train)).getMin());
    }

    @Test
    void shouldGetNumberOfRouteHopsBetweenManPiccAndLondonEustom() {
        assertEquals(0, routeToRouteCosts.getNumberOfChanges(manPicc, londonEuston, Collections.singleton(Train)).getMin());
    }

    @Test
    void shouldGetNumberOfRouteHopsBetweenAltrinchamAndManPicc() {
        Station altrincham = stationRepository.getStationById(Altrincham.getId());
        assertEquals(1, routeToRouteCosts.getNumberOfChanges(altrincham, londonEuston, Collections.singleton(Train)).getMin());
    }
}

package com.tramchester.integration.graph.rail;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.RouteCostCalculator;
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
public class RailRouteCostsTest {
    private static ComponentContainer componentContainer;

    private StationRepository stationRepository;
    private Transaction txn;
    private RouteCostCalculator routeCostCalculator;
    private Station stockport;
    private Station manPicc;
    private Station londonEuston;
    private Station wilmslow;
    private Station crewe;
    private Station miltonKeynes;

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
        stationRepository = componentContainer.get(StationRepository.class);
        routeCostCalculator = componentContainer.get(RouteCostCalculator.class);

        stockport = Stockport.getFrom(stationRepository);
        manPicc = ManchesterPiccadilly.getFrom(stationRepository);
        londonEuston = LondonEuston.getFrom(stationRepository);
        wilmslow = Wilmslow.getFrom(stationRepository);
        crewe = Crewe.getFrom(stationRepository);
        miltonKeynes = MiltonKeynesCentral.getFrom(stationRepository);
    }

    /***
     * May need graph rebuild if a fix made to data import code
     */

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldGetApproxCostBetweenStockportAndManPiccadilly() {
        assertEquals(8, routeCostCalculator.getApproxCostBetween(txn, stockport, manPicc));
    }

    @Test
    void shouldGetApproxCostBetweenManPiccadillyAndStockport() {
        assertEquals(6, routeCostCalculator.getApproxCostBetween(txn, manPicc, stockport));
    }

    @Test
    void shouldGetApproxCostBetweenStockportAndWilmslow() {
        assertEquals(6, routeCostCalculator.getApproxCostBetween(txn, stockport, wilmslow));
    }

    @Test
    void shouldGetApproxCostBetweenWilmslowAndCrewe() {
        assertEquals(15, routeCostCalculator.getApproxCostBetween(txn, wilmslow, crewe));
    }

    @Test
    void shouldGetApproxCostCreweAndMiltonKeeny() {
        assertEquals(63, routeCostCalculator.getApproxCostBetween(txn, crewe, miltonKeynes));
    }

    @Test
    void shouldGetApproxCostMiltonKeynesLondon() {
        assertEquals(32, routeCostCalculator.getApproxCostBetween(txn, miltonKeynes, londonEuston));
    }

    @Test
    void shouldGetApproxCostBetweenManPicadillyAndLondonEuston() {
        assertEquals(119, routeCostCalculator.getApproxCostBetween(txn, manPicc, londonEuston));
    }

    @Test
    void shouldGetApproxCostBetweenAltrinchamAndLondonEuston() {
        Station altrincham = Altrincham.getFrom(stationRepository);

        assertEquals(129, routeCostCalculator.getApproxCostBetween(txn, altrincham, londonEuston));
    }

}

package com.tramchester.integration.graph.rail;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.InvalidDurationException;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.RouteCostCalculator;
import com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.TrainTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.neo4j.graphdb.Transaction;

import java.time.Duration;

import static com.tramchester.integration.testSupport.rail.RailStationIds.*;
import static com.tramchester.testSupport.TestEnv.assertMinutesEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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

    private final TramServiceDate date = new TramServiceDate(TestEnv.testDay());

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
    void shouldGetApproxCostBetweenStockportAndManPiccadilly() throws InvalidDurationException {
        assertMinutesEquals(9, routeCostCalculator.getAverageCostBetween(txn, stockport, manPicc, date));
        assertMinutesEquals(10, routeCostCalculator.getMaxCostBetween(txn, stockport, manPicc, date));
    }

    @Test
    void shouldGetApproxCostBetweenManPiccadillyAndStockport() throws InvalidDurationException {
        assertMinutesEquals(7, routeCostCalculator.getAverageCostBetween(txn, manPicc, stockport, date));
    }

    @Test
    void shouldGetApproxCostBetweenStockportAndWilmslow() throws InvalidDurationException {
        assertMinutesEquals(7, routeCostCalculator.getAverageCostBetween(txn, stockport, wilmslow, date));
    }

    @Test
    void shouldGetApproxCostBetweenWilmslowAndCrewe() throws InvalidDurationException {
        assertMinutesEquals(17, routeCostCalculator.getAverageCostBetween(txn, wilmslow, crewe, date));
    }

    @Test
    void shouldGetApproxCostCreweAndMiltonKeeny() throws InvalidDurationException {
        assertMinutesEquals(67, routeCostCalculator.getAverageCostBetween(txn, crewe, miltonKeynes, date));
        assertMinutesEquals(67, routeCostCalculator.getMaxCostBetween(txn, crewe, miltonKeynes, date));
    }

    @Test
    void shouldGetApproxCostMiltonKeynesLondon() throws InvalidDurationException {
        assertMinutesEquals(33, routeCostCalculator.getAverageCostBetween(txn, miltonKeynes, londonEuston, date));
        assertMinutesEquals(34, routeCostCalculator.getMaxCostBetween(txn, miltonKeynes, londonEuston, date));
    }

    @Test
    void shouldGetApproxCostBetweenManPicadillyAndLondonEuston() throws InvalidDurationException {
        assertMinutesEquals(124, routeCostCalculator.getAverageCostBetween(txn, manPicc, londonEuston, date));
        assertMinutesEquals(124, routeCostCalculator.getMaxCostBetween(txn, manPicc, londonEuston, date));
    }

    @Test
    void shouldGetApproxCostBetweenAltrinchamAndLondonEuston() throws InvalidDurationException {
        Station altrincham = Altrincham.getFrom(stationRepository);

        assertMinutesEquals(135, routeCostCalculator.getAverageCostBetween(txn, altrincham, londonEuston, date));
        assertMinutesEquals(135, routeCostCalculator.getMaxCostBetween(txn, altrincham, londonEuston, date));

    }

    // There is a zero cost for this journey, but only between specific dates 24/1 until 27/1 2022
    // BS N X13514 220124 220127 1111000 5BR0B00    124748000                              N
    @Test
    void shouldReproIssueWithZeroCostLegs() throws InvalidDurationException {

        Station mulsecoomb = stationRepository.getStationById(StringIdFor.createId("MLSECMB"));
        Station londonRoadBrighton = stationRepository.getStationById(StringIdFor.createId("BRGHLRD"));
        Duration result = routeCostCalculator.getAverageCostBetween(txn, mulsecoomb, londonRoadBrighton, date);

        assertNotEquals(Duration.ZERO, result);
    }

}

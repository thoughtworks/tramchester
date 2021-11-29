package com.tramchester.integration.graph.rail;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.TrainTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tramchester.integration.testSupport.rail.RailStationIds.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

@TrainTest
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
public class RailRouteCalculatorTest {
    private static final int TXN_TIMEOUT = 5*60;
    private static StationRepository stationRepository;

    private final LocalDate when = TestEnv.testDay();

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;

    private Transaction txn;
    private RouteCalculatorTestFacade testFacade;
    private Station stockport;
    private Station manchesterPiccadilly;
    private Station altrincham;
    private Station londonEuston;
    private Station macclesfield;
    private TramTime travelTime;
    private Station stokeOnTrent;
    private Station miltonKeynesCentral;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig testConfig = new IntegrationRailTestConfig();
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        stationRepository = componentContainer.get(StationRepository.class);
        database = componentContainer.get(GraphDatabase.class);
    }

    @AfterEach
    void afterAllEachTestsHasRun() {
        txn.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        txn = database.beginTx(TXN_TIMEOUT, TimeUnit.SECONDS);
        testFacade = new RouteCalculatorTestFacade(componentContainer.get(RouteCalculator.class), stationRepository, txn);

        stockport = stationRepository.getStationById(Stockport.getId());
        manchesterPiccadilly = stationRepository.getStationById(ManchesterPiccadilly.getId());
        altrincham = stationRepository.getStationById(Altrincham.getId());
        londonEuston = stationRepository.getStationById(LondonEuston.getId());
        macclesfield = stationRepository.getStationById(Macclesfield.getId());
        stokeOnTrent = stationRepository.getStationById(StokeOnTrent.getId());
        miltonKeynesCentral = stationRepository.getStationById(MiltonKeynesCentral.getId());

        travelTime = TramTime.of(8, 0);

    }

    @Test
    void shouldHaveStockportToManPicc() {

        JourneyRequest request = new JourneyRequest(new TramServiceDate(when), travelTime, false, 1,
                30, 1);

        atLeastOneDirect(request, stockport, manchesterPiccadilly);
    }

    @Test
    void shouldHaveManPiccToStockport() {

        JourneyRequest request = new JourneyRequest(new TramServiceDate(when), travelTime, false, 0,
                30, 1);

        atLeastOneDirect(request, manchesterPiccadilly, stockport);
    }

    @Test
    void shouldHaveManPiccToMacclesfield() {

        JourneyRequest request = new JourneyRequest(new TramServiceDate(when), travelTime, false, 0,
                45, 1);

        atLeastOneDirect(request, manchesterPiccadilly, macclesfield);
    }

    @Test
    void shouldHaveManPiccToMiltonKeynesCentral() {
        JourneyRequest request = new JourneyRequest(new TramServiceDate(when), travelTime, false, 0,
                120, 1);

        atLeastOneDirect(request, manchesterPiccadilly, miltonKeynesCentral);
    }

    @Test
    void shouldHaveMiltonKeynesToManchester() {
        JourneyRequest request = new JourneyRequest(new TramServiceDate(when), travelTime, false, 0,
                120, 1);

        request.setDiag(true);

        atLeastOneDirect(request, miltonKeynesCentral, manchesterPiccadilly);
    }

    @Test
    void shouldHaveManPiccToStoke() {
        JourneyRequest request = new JourneyRequest(new TramServiceDate(when), travelTime, false, 0,
                80, 1);

        atLeastOneDirect(request, manchesterPiccadilly, stokeOnTrent);
    }

    @Test
    void shouldHaveAltrinchamToStockport() {

        JourneyRequest request = new JourneyRequest(new TramServiceDate(when), travelTime, false, 1,
                45, 1);

        atLeastOneDirect(request, altrincham, stockport);
    }

    @Test
    void shouldHaveManchesterToLondonEuston() {

        JourneyRequest request = new JourneyRequest(new TramServiceDate(when), travelTime, false, 0,
                240, 3);

        atLeastOneDirect(request, manchesterPiccadilly, londonEuston);
    }

    @Disabled("performance")
    @Test
    void shouldHaveAltrinchamToLondonEuston() {
        TramTime travelTime = TramTime.of(8, 0);

        JourneyRequest request = new JourneyRequest(new TramServiceDate(when), travelTime, false, 2,
                240, 1);

        atLeastOneDirect(request, altrincham, londonEuston);
    }

    private void atLeastOneDirect(JourneyRequest request, Station start, Station dest) {
        Set<Journey> journeys = testFacade.calculateRouteAsSet(start, dest, request);
        assertFalse(journeys.isEmpty());

        // At least one direct
        List<Journey> direct = journeys.stream().filter(journey -> journey.getStages().size() == 1).collect(Collectors.toList());
        assertFalse(direct.isEmpty(), "No direct from " + start + " to " + dest);
    }


}
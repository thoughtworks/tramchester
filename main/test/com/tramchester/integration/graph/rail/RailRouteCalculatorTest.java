package com.tramchester.integration.graph.rail;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
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
import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void shouldHaveHaleToKnutsford() {
        TramTime travelTime = TramTime.of(9, 0);

        JourneyRequest request = new JourneyRequest(new TramServiceDate(when), travelTime, false, 1,
                30, 1);
        atLeastOneDirect(request, Hale.getId(), Knutsford.getId());
    }

    @Test
    void shouldHaveKnutsfordToHale9am() {
        TramTime travelTime = TramTime.of(9, 0);

        JourneyRequest request = new JourneyRequest(new TramServiceDate(when), travelTime, false, 1,
                120, 1);

        atLeastOneDirect(request, Knutsford.getId(), Hale.getId());
    }

    @Test
    void shouldFindCorrectNumberOfJourneys() {

        // TODO this tests causes timeouts in the search algo, when maxNumberJourneys >1 need to track down why

        TramTime travelTime = TramTime.of(11,4);

        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(when), travelTime, false, 3,
                3*60, 2);

        //journeyRequest.setDiag(true);

        Set<Journey> results = testFacade.calculateRouteAsSet(Derby.getId(), Altrincham.getId(), journeyRequest);

        assertEquals(1, results.size(), results.toString());
    }

    @Test
    void shouldHaveSimpleJourneyEustonToManchester() {
        TramTime travelTime = TramTime.of(8, 0);

        JourneyRequest request = new JourneyRequest(new TramServiceDate(when), travelTime, false, 0,
                3*60, 3);
        Set<Journey> journeys = testFacade.calculateRouteAsSet(RailStationIds.LondonEuston.getId(),
                ManchesterPiccadilly.getId(),
                request);
        assertFalse(journeys.isEmpty());

        journeys.forEach(journey -> {
            List<TransportStage<?, ?>> stages = journey.getStages();
            assertEquals(1, stages.size());

            TransportStage<?, ?> trainStage = stages.get(0);

            assertEquals(TransportMode.Train, trainStage.getMode());
            final int passedStopsCount = trainStage.getPassedStopsCount();
            assertEquals(3, passedStopsCount, trainStage.toString());

            List<StopCall> callingPoints = trainStage.getCallingPoints();
            final int numCallingPoints = callingPoints.size();
            assertTrue(numCallingPoints==3 || numCallingPoints==4, callingPoints.toString());

            if (numCallingPoints==4) {
                // milton K -> Stoke -> Macclesfield -> Stockport
                assertEquals(MiltonKeynesCentral.getId(), callingPoints.get(0).getStationId());
                assertEquals(StokeOnTrent.getId(), callingPoints.get(1).getStationId());
                assertEquals(Macclesfield.getId(), callingPoints.get(2).getStationId());
                assertEquals(Stockport.getId(), callingPoints.get(3).getStationId());
            } else {
                // crewe -> wilmslow -> stockport OR Milton Keynes, Stoke, stockport
                //assertEquals(Crewe.getId(), callingPoints.get(0).getStationId());
                //assertEquals(Wilmslow.getId(), callingPoints.get(1).getStationId());
                assertEquals(Stockport.getId(), callingPoints.get(2).getStationId());
            }
        });
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
        atLeastOneDirect(request, start.getId(), dest.getId());
    }

    private void atLeastOneDirect(JourneyRequest request, IdFor<Station> start, IdFor<Station> dest) {
        Set<Journey> journeys = testFacade.calculateRouteAsSet(start, dest, request);
        assertFalse(journeys.isEmpty());

        // At least one direct
        List<Journey> direct = journeys.stream().filter(journey -> journey.getStages().size() == 1).collect(Collectors.toList());
        assertFalse(direct.isEmpty(), "No direct from " + start + " to " + dest);
    }


}

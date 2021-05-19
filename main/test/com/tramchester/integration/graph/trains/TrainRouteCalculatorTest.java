package com.tramchester.integration.graph.trains;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.graph.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.train.IntegrationTrainTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TrainStations;
import com.tramchester.testSupport.testTags.TrainTest;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.reference.TrainStations.*;
import static org.junit.jupiter.api.Assertions.*;

@TrainTest
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class TrainRouteCalculatorTest {
    // TODO this needs to be > time for whole test fixture, see note below in @After
    private static final int TXN_TIMEOUT = 5*60;

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private RouteCalculatorTestFacade testFacade;

    private final LocalDate when = TestEnv.testDay();
    private Transaction txn;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig testConfig = new IntegrationTrainTestConfig();
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        database = componentContainer.get(GraphDatabase.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        txn = database.beginTx(TXN_TIMEOUT, TimeUnit.SECONDS);
        StationRepository stationRepository = componentContainer.get(StationRepository.class);
        testFacade = new RouteCalculatorTestFacade(componentContainer.get(RouteCalculator.class), stationRepository, txn);
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldHaveSimpleJourneyEustonToManchester() {
        TramTime travelTime = TramTime.of(8, 0);

        JourneyRequest request = new JourneyRequest(new TramServiceDate(when), travelTime, false, 0,
                3*60);
        Set<Journey> journeys = testFacade.calculateRouteAsSet(TrainStations.LondonEuston, ManchesterPiccadilly,
                request, 3);
        assertFalse(journeys.isEmpty());

        journeys.forEach(journey -> {
            List<TransportStage<?, ?>> stages = journey.getStages();
            assertEquals(1, stages.size());

            TransportStage<?, ?> trainStage = stages.get(0);

            assertEquals(TransportMode.Train, trainStage.getMode());
            assertEquals(19, trainStage.getPassedStopsCount(), trainStage.toString());

            List<StopCall> callingPoints = trainStage.getCallingPoints();
            final int numCallingPoints = callingPoints.size();
            assertTrue(numCallingPoints==3 || numCallingPoints==4, callingPoints.toString());
            if (numCallingPoints==4) {
                // milton K -> Stoke -> Macclesfield -> Stockport
                assertEquals("MKC", callingPoints.get(0).getStationId().forDTO());
                assertEquals("SOT", callingPoints.get(1).getStationId().forDTO());
                assertEquals("MAC", callingPoints.get(2).getStationId().forDTO());
                assertEquals("SPT", callingPoints.get(3).getStationId().forDTO());
            } else {
                // crewe -> wilmslow -> stockport
                assertEquals("CRE", callingPoints.get(0).getStationId().forDTO());
                assertEquals("WML", callingPoints.get(1).getStationId().forDTO());
                assertEquals("SPT", callingPoints.get(2).getStationId().forDTO());
            }
        });
    }

    @Test
    void shouldHaveStockportToManPicc() {
        TramTime travelTime = TramTime.of(8, 0);

        JourneyRequest request = new JourneyRequest(new TramServiceDate(when), travelTime, false, 1,
                30);

        atLeastOneDirect(request, Stockport, ManchesterPiccadilly);
    }

    @Test
    void shouldHaveHaleToKnutsford() {
        TramTime travelTime = TramTime.of(9, 0);

        JourneyRequest request = new JourneyRequest(new TramServiceDate(when), travelTime, false, 1,
                30);
        atLeastOneDirect(request, Hale, Knutsford);
    }

    @Test
    void shouldHaveKnutsfordToHale() {
        TramTime travelTime = TramTime.of(9, 0);

        JourneyRequest request = new JourneyRequest(new TramServiceDate(when), travelTime, false, 1,
                30);
        atLeastOneDirect(request, Knutsford, Hale);
    }


    @NotNull
    private List<Pair<IdFor<Station>, IdFor<Station>>> queryForJourneys(JourneyRequest request, IdSet<Station> stationIds) {
        List<Pair<IdFor<Station>, IdFor<Station>>> failed = new ArrayList<>();
        for(IdFor<Station> begin : stationIds) {
            for(IdFor<Station> end : stationIds) {
                if (!begin.equals(end)) {
                    Set<Journey> journeys = testFacade.calculateRouteAsSet(begin, end, request, 1);
                    if (journeys.isEmpty()) {
                        failed.add(Pair.of(begin,end));
                    }
                }
            }
        }
        return failed;
    }

    private void atLeastOneDirect(JourneyRequest request, TrainStations start, TrainStations dest) {
        Set<Journey> journeys = testFacade.calculateRouteAsSet(start, dest, request, 1);
        assertFalse(journeys.isEmpty());

        // At least one direct
        List<Journey> direct = journeys.stream().filter(journey -> journey.getStages().size() == 1).collect(Collectors.toList());
        assertFalse(direct.isEmpty(), "No direct from " + start + " to " + dest);
    }
}

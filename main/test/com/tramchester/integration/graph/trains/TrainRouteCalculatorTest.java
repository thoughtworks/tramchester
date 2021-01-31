package com.tramchester.integration.graph.trains;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.testSupport.IntegrationTrainTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.RouteCalculatorTestFacade;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TrainStations;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class TrainRouteCalculatorTest {
    // TODO this needs to be > time for whole test fixture, see note below in @After
    private static final int TXN_TIMEOUT = 5*60;

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private RouteCalculatorTestFacade calculator;

    private final LocalDate when = TestEnv.testDay();
    private Transaction txn;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig testConfig = new IntegrationTrainTestConfig();
        componentContainer = new ComponentsBuilder<>().create(testConfig, TestEnv.NoopRegisterMetrics());
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
        calculator = new RouteCalculatorTestFacade(componentContainer.get(RouteCalculator.class), stationRepository, txn);
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldHaveSimpleJourneyEustonToManchester() {
        TramTime travelTime = TramTime.of(8, 0);

        JourneyRequest request = new JourneyRequest(new TramServiceDate(when), travelTime, false, 1,
                3*60);
        Set<Journey> journeys = calculator.calculateRouteAsSet(TrainStations.LondonEuston, TrainStations.ManchesterPiccadilly,
                request, 3);
        assertFalse(journeys.isEmpty());

        // At least one direct
        List<Journey> direct = journeys.stream().filter(journey -> journey.getStages().size() == 1).collect(Collectors.toList());
        assertFalse(direct.isEmpty());

        List<TransportStage<?, ?>> stages = direct.get(1).getStages();
        assertEquals(1, stages.size());

        TransportStage<?, ?> trainStage = stages.get(0);

        assertEquals(trainStage.getPassedStopsCount(), 21, trainStage.toString());
        List<StopCall> callingPoints = trainStage.getCallingPoints();

        List<StopCall> dropOffs = callingPoints.stream().
                filter(stopCall -> stopCall.getDropoffType().equals(GTFSPickupDropoffType.Regular)).
                collect(Collectors.toList());
        assertEquals(4, dropOffs.size());
        assertEquals("MKC", dropOffs.get(0).getStationId().forDTO());
        assertEquals("SOT", dropOffs.get(1).getStationId().forDTO());
        assertEquals("MAC", dropOffs.get(2).getStationId().forDTO());
        assertEquals("SPT", dropOffs.get(3).getStationId().forDTO());

    }
}

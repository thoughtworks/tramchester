package com.tramchester.integration.graph.railAndTram;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.NumberOfChanges;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.graph.search.RouteToRouteCosts;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.TramAndTrainGreaterManchesterConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.TrainTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Train;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.integration.testSupport.rail.RailStationIds.ManchesterPiccadilly;
import static com.tramchester.integration.testSupport.rail.RailStationIds.Stockport;
import static org.junit.jupiter.api.Assertions.*;

@TrainTest
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
public class RailAndTramRouteCalculatorTest {
    private static final int TXN_TIMEOUT = 5*60;
    private static StationRepository stationRepository;

    private final LocalDate when = TestEnv.testDay();

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;

    private Transaction txn;
    private RouteCalculatorTestFacade testFacade;

    private TramTime travelTime;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig testConfig = new TramAndTrainGreaterManchesterConfig();
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        stationRepository = componentContainer.get(StationRepository.class);
        database = componentContainer.get(GraphDatabase.class);
    }

    @AfterEach
    void afterAllEachTestsHasRun() {
        txn.close();
    }

    @AfterAll
    static void afterAllTestsRun() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        txn = database.beginTx(TXN_TIMEOUT, TimeUnit.SECONDS);
        testFacade = new RouteCalculatorTestFacade(componentContainer.get(RouteCalculator.class), stationRepository, txn);

        travelTime = TramTime.of(8, 0);
    }

    @Test
    void  shouldHaveTrainsAndTramStationsInRepos() {
        assertTrue(stationRepository.hasStationId(ManchesterPiccadilly.getId()));
        assertTrue(stationRepository.hasStationId(TramStations.ExchangeSquare.getId()));
        assertTrue(stationRepository.hasStationId(TramStations.Altrincham.getId()));
    }

    @Test
    void shouldHaveMaxHopsBetweenTramAndRail() {
        RouteToRouteCosts routeToRouteCosts = componentContainer.get(RouteToRouteCosts.class);
        NumberOfChanges result = routeToRouteCosts.getNumberOfChanges(tram(TramStations.Bury), rail(Stockport));

        assertTrue(result.getMin()!=Integer.MAX_VALUE);
        assertTrue(result.getMax()!=Integer.MAX_VALUE);
    }

    @Test
    void shouldHaveStockportToManPiccViaRail() {

        JourneyRequest request = new JourneyRequest(new TramServiceDate(when), travelTime, false, 1,
                30, 1);

        atLeastOneDirect(request, rail(Stockport), rail(ManchesterPiccadilly), Train);
    }

    @Test
    void shouldHaveManPiccToStockportViaRail() {

        JourneyRequest request = new JourneyRequest(new TramServiceDate(when), travelTime, false, 0,
                30, 1);

        atLeastOneDirect(request, rail(ManchesterPiccadilly), rail(Stockport), Train);
    }

    @Test
    void shouldHaveAltyToStPetersSquareViaTram() {
        JourneyRequest request = new JourneyRequest(new TramServiceDate(when), travelTime, false, 0,
                30, 1);

        atLeastOneDirect(request, tram(TramStations.Altrincham), tram(TramStations.StPetersSquare), Tram);
    }

    @Disabled("WIP")
    @Test
    void shouldBuryToStockportViaTramAndTrain() {
        JourneyRequest request = new JourneyRequest(new TramServiceDate(when), travelTime, false, 0,
                30, 1);

        Set<Journey> journeys = testFacade.calculateRouteAsSet(tram(TramStations.Bury), rail(Stockport), request);
        assertFalse(journeys.isEmpty());
    }

    private Station rail(RailStationIds railStation) {
        return railStation.getFrom(stationRepository);
    }

    private Station tram(TramStations tramStation) {
        return tramStation.from(stationRepository);
    }

    private void atLeastOneDirect(JourneyRequest request, Station start, Station dest, TransportMode mode) {
        Set<Journey> journeys = testFacade.calculateRouteAsSet(start, dest, request);
        assertFalse(journeys.isEmpty());

        // At least one direct
        List<Journey> direct = journeys.stream().filter(journey -> journey.getStages().size() == 1).collect(Collectors.toList());
        assertFalse(direct.isEmpty(), "No direct from " + start + " to " + dest);

        direct.forEach(journey -> {
            journey.getStages().forEach(stage -> assertEquals(mode, stage.getMode(),
                    "Mode wrong for journey " + journey + " for request " + request));
        });

    }



}

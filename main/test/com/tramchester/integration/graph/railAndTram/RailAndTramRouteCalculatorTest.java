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
import com.tramchester.repository.NeighboursRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.TrainTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.neo4j.graphdb.Transaction;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.*;
import static com.tramchester.integration.testSupport.rail.RailStationIds.*;
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
    void shouldValidHopsBetweenTramAndRail() {
        RouteToRouteCosts routeToRouteCosts = componentContainer.get(RouteToRouteCosts.class);
        NumberOfChanges result = routeToRouteCosts.getNumberOfChanges(tram(TramStations.Bury), rail(Stockport), Collections.emptySet());

        assertTrue(result.getMin()!=Integer.MAX_VALUE);
        assertTrue(result.getMax()!=Integer.MAX_VALUE);
    }

    @Test
    void shouldNotHaveHopsBetweenTramAndRailWhenTramOnly() {
        RouteToRouteCosts routeToRouteCosts = componentContainer.get(RouteToRouteCosts.class);
        NumberOfChanges result = routeToRouteCosts.getNumberOfChanges(tram(TramStations.Bury), rail(Stockport),
                Collections.singleton(Tram));

        assertEquals(Integer.MAX_VALUE, result.getMin());
        assertEquals(Integer.MAX_VALUE, result.getMax());
    }

    @Test
    void shouldReproIssueWithInvalidTimes() {
        TramTime time = TramTime.of(10,49);
        JourneyRequest request = new JourneyRequest(new TramServiceDate(when), time, false, 3,
                Duration.ofMinutes(30), 1, getRequestedModes());

        // ashton west
        Station start = rail(Altrincham);
        Station dest = tram(TramStations.Ashton);

        Set<Journey> journeys = testFacade.calculateRouteAsSet(start, dest, request);
        assertFalse(journeys.isEmpty());

    }

    @Test
    void shouldHaveStockportToManPiccViaRail() {

        JourneyRequest request = new JourneyRequest(new TramServiceDate(when), travelTime, false, 1,
                Duration.ofMinutes(30), 1, getRequestedModes());

        atLeastOneDirect(request, rail(Stockport), rail(ManchesterPiccadilly), Train);
    }

    private Set<TransportMode> getRequestedModes() {
        // empty means all
        return Collections.emptySet();
    }

    @Test
    void shouldHaveManPiccToStockportViaRail() {

        JourneyRequest request = new JourneyRequest(new TramServiceDate(when), travelTime, false, 0,
                Duration.ofMinutes(30), 1, getRequestedModes());

        atLeastOneDirect(request, rail(ManchesterPiccadilly), rail(Stockport), Train);
    }

    @Test
    void shouldNotHaveManPiccToStockportWhenTramOnly() {

        JourneyRequest request = new JourneyRequest(new TramServiceDate(when), travelTime, false, 0,
                Duration.ofMinutes(30), 1, Collections.singleton(Tram));

        Set<Journey> journeys = testFacade.calculateRouteAsSet(rail(ManchesterPiccadilly), rail(Stockport), request);
        assertTrue(journeys.isEmpty());

    }

    @Test
    void shouldHaveAltyToStPetersSquareViaTram() {
        JourneyRequest request = new JourneyRequest(new TramServiceDate(when), travelTime, false, 0,
                Duration.ofMinutes(30), 1, getRequestedModes());

        atLeastOneDirect(request, tram(TramStations.Altrincham), tram(TramStations.StPetersSquare), Tram);
    }

    @Test
    void shouldHaveNeighboursFromConfig() {
        NeighboursRepository neighboursRepository = componentContainer.get(NeighboursRepository.class);

        Station eastDidsburyRail = RailStationIds.EastDidsbury.from(stationRepository);
        Station eastDidsburyTram = TramStations.EastDidsbury.from(stationRepository);

        Set<Station> neighbours = neighboursRepository.getNeighboursFor(eastDidsburyTram.getId());
        assertEquals(1, neighbours.size());

        assertTrue(neighbours.contains(eastDidsburyRail));

        neighbours = neighboursRepository.getNeighboursFor(eastDidsburyRail.getId());
        assertEquals(1, neighbours.size());

        assertTrue(neighbours.contains(eastDidsburyTram));
    }

    @Test
    void shouldHaveWalKFromDidsburyTramToDidsburyTrain() {
        JourneyRequest request = new JourneyRequest(new TramServiceDate(when), travelTime, false, 0,
                Duration.ofMinutes(30), 1, getRequestedModes());

        Station start = tram(TramStations.EastDidsbury);
        Station dest = RailStationIds.EastDidsbury.from(stationRepository);
        Set<Journey> journeys = testFacade.calculateRouteAsSet(start, dest, request);
        assertFalse(journeys.isEmpty());

        // At least one direct
        List<Journey> direct = journeys.stream().filter(journey -> journey.getStages().size() == 1).collect(Collectors.toList());
        assertFalse(direct.isEmpty(), "No direct from " + start + " to " + dest);

        direct.forEach(journey -> {
            journey.getStages().forEach(stage -> assertEquals(Connect, stage.getMode(),
                    "Mode wrong for journey " + journey + " for request " + request));
        });

    }

    @Test
    void shouldBuryToStockportViaTramAndTrain() {
        JourneyRequest request = new JourneyRequest(new TramServiceDate(when), travelTime, false, 2,
                Duration.ofMinutes(110), 1, getRequestedModes());

        Set<Journey> journeys = testFacade.calculateRouteAsSet(tram(TramStations.Bury), rail(Stockport), request);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldHaveMultistageTest() {
        // reproduces failing scenario from Acceptance tests
        //   TramTime planTime = TramTime.of(10,0);
        //        desiredJourney(appPage, altrincham, TramStations.ManAirport.getName(), when, planTime, false);

        JourneyRequest request = new JourneyRequest(new TramServiceDate(when), TramTime.of(10,0), false, 2,
                Duration.ofMinutes(110), 1, getRequestedModes());

        Set<Journey> results = testFacade.calculateRouteAsSet(TramStations.Altrincham, TramStations.ManAirport, request);
        assertFalse(results.isEmpty());
    }

    private Station rail(RailStationIds railStation) {
        return railStation.from(stationRepository);
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

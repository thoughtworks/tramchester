package com.tramchester.integration.graph.railAndTram;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.RailAndTramGreaterManchesterConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.repository.NeighboursRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.GMTest;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.*;
import static com.tramchester.integration.testSupport.rail.RailStationIds.*;
import static com.tramchester.integration.testSupport.rail.RailStationIds.Altrincham;
import static com.tramchester.testSupport.reference.TramStations.*;
import static com.tramchester.testSupport.reference.TramStations.Eccles;
import static org.junit.jupiter.api.Assertions.*;

@GMTest
public class RailAndTramRouteCalculatorTest {
    private static final int TXN_TIMEOUT = 5*60;
    private static StationRepository stationRepository;
    private static RailAndTramGreaterManchesterConfig config;

    private final TramDate when = TestEnv.testDay();

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;

    private Transaction txn;
    private RouteCalculatorTestFacade testFacade;

    private TramTime travelTime;
    private Duration maxDurationFromConfig;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new RailAndTramGreaterManchesterConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
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

        maxDurationFromConfig = Duration.ofMinutes(config.getMaxJourneyDuration());

    }

    @Test
    void  shouldHaveTrainsAndTramStationsInRepos() {
        assertTrue(stationRepository.hasStationId(ManchesterPiccadilly.getId()));
        assertTrue(stationRepository.hasStationId(TramStations.ExchangeSquare.getId()));
        assertTrue(stationRepository.hasStationId(TramStations.Altrincham.getId()));
    }

    @Test
    void reproIssueRochdaleToEccles() {
        // this works fine when only tram data loaded, but fails when tram and train is loaded
        TramTime time = TramTime.of(9,0);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, 1, maxDurationFromConfig,
                1, TramsOnly);

        Set<Journey> journeys = testFacade.calculateRouteAsSet(Rochdale, Eccles, journeyRequest);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldHaveRochdaleToStPetersSquare() {
        TramTime time = TramTime.of(9,0);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, 0, maxDurationFromConfig,
                1, TramsOnly);

        Set<Journey> journeys = testFacade.calculateRouteAsSet(Rochdale, StPetersSquare, journeyRequest);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldHaveStPetersSquareToEccles() {
        TramTime time = TramTime.of(9,0);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, 0, maxDurationFromConfig,
                1, TramsOnly);

        Set<Journey> journeys = testFacade.calculateRouteAsSet(StPetersSquare, Eccles, journeyRequest);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldHaveVictoriaToEccles() {
        // this works fine when only tram data loaded, but fails when tram and train is loaded
        TramTime time = TramTime.of(9,0);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, 1, maxDurationFromConfig,
                1, TramsOnly);

        journeyRequest.setDiag(true);

        Set<Journey> journeys = testFacade.calculateRouteAsSet(Victoria, Eccles, journeyRequest);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldHaveVictoriaToEcclesTrainAndTramAllowed() {
        // check if allowing all transport modes makes a difference.....
        // this works fine when only tram data loaded, but fails when tram and train is loaded
        TramTime time = TramTime.of(9,0);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, 1, maxDurationFromConfig,
                1, EnumSet.of(Tram, Train));

        journeyRequest.setDiag(true);

        Set<Journey> journeys = testFacade.calculateRouteAsSet(Victoria, Eccles, journeyRequest);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldHaveDeangateToEccles() {
        // check if failing when TramsOnly and nearby rail station
        // this works fine when only tram data loaded, but fails when tram and train is loaded
        TramTime time = TramTime.of(9,0);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, 1, maxDurationFromConfig,
                1, TramsOnly);

        //journeyRequest.setDiag(true);

        Set<Journey> journeys = testFacade.calculateRouteAsSet(Deansgate, Eccles, journeyRequest);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldHaveExchangeSqToEccles() {
        TramTime time = TramTime.of(9,0);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, 1, maxDurationFromConfig,
                1, TramsOnly);

        Set<Journey> journeys = testFacade.calculateRouteAsSet(ExchangeSquare, Eccles, journeyRequest);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldHaveMarketStreetToEccles() {
        TramTime time = TramTime.of(9,0);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, 1, maxDurationFromConfig,
                1, TramsOnly);

        Set<Journey> journeys = testFacade.calculateRouteAsSet(MarketStreet, Eccles, journeyRequest);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldReproIssueWithInvalidTimes() {
        TramTime time = TramTime.of(10,49);
        JourneyRequest request = new JourneyRequest(when, time, false, 3,
                Duration.ofMinutes(30), 1, tramAndTrain());

        // ashton west
        Station start = rail(Altrincham);
        Station dest = tram(TramStations.Ashton);

        Set<Journey> journeys = testFacade.calculateRouteAsSet(start, dest, request);
        assertFalse(journeys.isEmpty(), "no journeys");

    }

    @Disabled("No way to detect duplicate at this level?")
    @Test
    void shouldReproIssueWithDuplicatedJourneyWhenMaxJoruneysIs5() {

        // TODO revisit this - although it does not a cause an actual issue since duplicates filtered out during mapping to DTOs

        TramTime time = TramTime.of(14,25);

        TramDate date = TramDate.of(2022, 10,14);

        // get a duplicate journey when set to 5 here...
        JourneyRequest request = new JourneyRequest(date, time, false, 1,
                Duration.ofMinutes(240), 5, EnumSet.of(Tram, Train));

        Station start = rail(Altrincham);
        Station dest = rail(Stockport);

        Set<Journey> journeys = testFacade.calculateRouteAsSet(start, dest, request);

        assertEquals(1, journeys.size(), "unexpected number of journeys " + journeys);

    }

    @Test
    void shouldHaveWalkBetweenAdjacentTramAndTrainStation() {
        TramTime time = TramTime.of(14,25);

        TramDate date = TestEnv.testDay();

        JourneyRequest request = new JourneyRequest(date, time, false, 0,
                Duration.ofMinutes(3), 1, EnumSet.of(Tram, Train));

        List<Journey> journeysFromTram = new ArrayList<>(testFacade.calculateRouteAsSet(tram(TramStations.Altrincham),
                rail(Altrincham), request));
        List<Journey> journeysFromTrain = new ArrayList<>(testFacade.calculateRouteAsSet(rail(Altrincham),
                tram(TramStations.Altrincham), request));

        assertEquals(1, journeysFromTram.size());
        assertEquals(1, journeysFromTrain.size());

        Journey fromTramJoruney = journeysFromTram.get(0);
        List<TransportStage<?, ?>> fromTramStages = fromTramJoruney.getStages();
        assertEquals(1, fromTramStages.size());

        Journey fromTrainJoruney = journeysFromTram.get(0);
        List<TransportStage<?, ?>> fromTrainStages = fromTrainJoruney.getStages();
        assertEquals(1, fromTrainStages.size());

        TransportStage<?, ?> fromTram = fromTramStages.get(0);
        assertEquals(Connect, fromTram.getMode());
        assertEquals(Duration.ofMinutes(1), fromTram.getDuration());

        TransportStage<?, ?> fromTrain = fromTrainStages.get(0);
        assertEquals(Connect, fromTrain.getMode());
        assertEquals(Duration.ofMinutes(1), fromTrain.getDuration());
    }

    @Test
    void shouldTakeDirectTrainWhenAvailable() {
        TramTime time = TramTime.of(14,25);

        //TramDate date = TramDate.of(2022, 10,14);
        JourneyRequest request = new JourneyRequest(when, time, false, 1,
                Duration.ofMinutes(240), 1, EnumSet.of(Tram, Train));

        Station start = rail(Altrincham);
        Station dest = rail(Stockport);

        Set<Journey> journeys = testFacade.calculateRouteAsSet(start, dest, request);
        assertEquals(1, journeys.size(), "unexpected number of journeys " + journeys);

        journeys.forEach(journey -> {
            List<TransportStage<?, ?>> stages = journey.getStages();
            assertEquals(1, stages.size(), "too many stages " + journey);
            assertEquals(stages.get(0).getMode(), Train, "wrong second stage for " + stages);
        });

    }

    @Test
    void shouldTakeDirectTrainWhenStarAtTramStationNextToStation() {
        TramTime time = TramTime.of(14,25);

        JourneyRequest request = new JourneyRequest(when, time, false, 1,
                Duration.ofMinutes(240), 3, tramAndTrain());

        Station start = tram(TramStations.Altrincham); // TRAM
        Station dest = rail(Stockport);

        List<Journey> journeys = new ArrayList<>(testFacade.calculateRouteAsSet(start, dest, request));
        assertFalse(journeys.isEmpty(), "no journeys");

        Journey journey = journeys.get(0);

        List<TransportStage<?, ?>> stages = journey.getStages();
        assertEquals(2, stages.size(),  "too many stages " + journeys);
        assertEquals(stages.get(0).getMode(), Connect, "wrong first stage for " + stages);
        assertEquals(stages.get(1).getMode(), Train, "wrong second stage for " + stages);

    }

    @Disabled("Not a realistic scenario? start from a tram station but select train only?")
    @Test
    void shouldTakeDirectTrainViaWalkWhenOnlyTrainModeSelected() {
        TramTime time = TramTime.of(14,25);

        EnumSet<TransportMode> trainOnly = EnumSet.of(Train);

        TramDate date = TramDate.of(2022, 10,14);
        JourneyRequest request = new JourneyRequest(date, time, false, 1,
                Duration.ofMinutes(240), 1, trainOnly);

        Station start = tram(TramStations.Altrincham);
        Station dest = rail(Stockport);

        List<Journey> journeys = new ArrayList<>(testFacade.calculateRouteAsSet(start, dest, request));
        assertFalse(journeys.isEmpty(), "no journeys");

        Journey journey = journeys.get(0);

        List<TransportStage<?, ?>> stages = journey.getStages();
        assertEquals(2, stages.size(),  "too many stages " + journey);
        assertEquals(stages.get(0).getMode(), Connect, "wrong first stage for " + stages);
        assertEquals(stages.get(1).getMode(), Train, "wrong second stage for " + stages);

    }


    @Test
    void shouldHaveStockportToManPiccViaRail() {

        JourneyRequest request = new JourneyRequest(when, travelTime, false, 1,
                Duration.ofMinutes(30), 1, tramAndTrain());

        atLeastOneDirect(request, rail(Stockport), rail(ManchesterPiccadilly), Train);
    }

    private EnumSet<TransportMode> tramAndTrain() {
        return EnumSet.of(Tram, Train, RailReplacementBus);
    }

    @Test
    void shouldHaveManPiccToStockportViaRail() {

        JourneyRequest request = new JourneyRequest(when, travelTime, false, 0,
                Duration.ofMinutes(30), 1, tramAndTrain());

        atLeastOneDirect(request, rail(ManchesterPiccadilly), rail(Stockport), Train);
    }

    @Test
    void shouldNotHaveManPiccToStockportWhenTramOnly() {

        JourneyRequest request = new JourneyRequest(when, travelTime, false, 0,
                Duration.ofMinutes(30), 1, EnumSet.of(Tram));

        Set<Journey> journeys = testFacade.calculateRouteAsSet(rail(ManchesterPiccadilly), rail(Stockport), request);
        assertTrue(journeys.isEmpty());

    }

    @Test
    void shouldHaveAltyToStPetersSquareViaTram() {
        JourneyRequest request = new JourneyRequest(when, travelTime, false, 0,
                Duration.ofMinutes(30), 1, tramAndTrain());

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
        JourneyRequest request = new JourneyRequest(when, travelTime, false, 0,
                Duration.ofMinutes(30), 1, tramAndTrain());

        Station start = tram(TramStations.EastDidsbury);
        Station dest = RailStationIds.EastDidsbury.from(stationRepository);
        Set<Journey> journeys = testFacade.calculateRouteAsSet(start, dest, request);
        assertFalse(journeys.isEmpty());

        // At least one direct
        List<Journey> direct = journeys.stream().filter(journey -> journey.getStages().size() == 1).collect(Collectors.toList());
        assertFalse(direct.isEmpty(), "No direct from " + start + " to " + dest);

        direct.forEach(journey -> journey.getStages().forEach(stage -> assertEquals(Connect, stage.getMode(),
                "Mode wrong for journey " + journey + " for request " + request)));

    }

    @Test
    void shouldBuryToStockportViaTramAndTrain() {
        JourneyRequest request = new JourneyRequest(when, travelTime, false, 2,
                Duration.ofMinutes(110), 1, tramAndTrain());

        Set<Journey> journeys = testFacade.calculateRouteAsSet(tram(TramStations.Bury), rail(Stockport), request);
        assertFalse(journeys.isEmpty(),"no journeys");
    }

    @Test
    void shouldHaveMultistageTest() {
        // reproduces failing scenario from Acceptance tests
        //   TramTime planTime = TramTime.of(10,0);
        //        desiredJourney(appPage, altrincham, TramStations.ManAirport.getName(), when, planTime, false);

        JourneyRequest request = new JourneyRequest(when, TramTime.of(10,0), false, 2,
                Duration.ofMinutes(110), 1, tramAndTrain());

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

        direct.forEach(journey -> journey.getStages().forEach(stage -> assertEquals(mode, stage.getMode(),
                "Mode wrong for journey " + journey + " for request " + request)));

    }



}

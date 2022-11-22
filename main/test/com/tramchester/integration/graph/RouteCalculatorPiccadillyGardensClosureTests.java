package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.ClosedStationsRepository;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.PiccGardens2022;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.tramchester.domain.reference.TransportMode.TramsOnly;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RouteCalculatorPiccadillyGardensClosureTests {
    private static final int TXN_TIMEOUT = 5*60;

    private static ComponentContainer componentContainer;
    private static TramchesterConfig testConfig;
    private static GraphDatabase database;

    private TramDate when;

    private Duration maxJourneyDuration;
    private RouteCalculatorTestFacade calculator;
    private StationRepository stationRepository;
    private Set<TransportMode> modes;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        testConfig = new IntegrationTramTestConfig();

        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
        database = componentContainer.get(GraphDatabase.class);

        TestEnv.clearDataCache(componentContainer);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        TestEnv.clearDataCache(componentContainer);
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {

        Transaction txn = database.beginTx(TXN_TIMEOUT, TimeUnit.SECONDS);

        maxJourneyDuration = Duration.ofMinutes(testConfig.getMaxJourneyDuration());
        stationRepository = componentContainer.get(StationRepository.class);

        calculator = new RouteCalculatorTestFacade(componentContainer.get(RouteCalculator.class), stationRepository, txn);

        when = TestEnv.testDay();

        modes = TramsOnly;
    }

    @Test
    void shouldHaveClosure() {
        ClosedStationsRepository closedStationsRepository = componentContainer.get(ClosedStationsRepository.class);

        Station piccGardens = PiccadillyGardens.from(stationRepository);

        assertTrue(closedStationsRepository.isClosed(piccGardens, when));
    }

    @Test
    void shouldHaveAltrinchamToBuryNormally() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,5), false,
                3, maxJourneyDuration, 5, modes);

        Set<Journey> journeys = calculator.calculateRouteAsSet(Altrincham, Bury, journeyRequest);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldHaveAshtonToCrumpsall() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,5), false,
                3, maxJourneyDuration, 5, modes);

        Set<Journey> journeys = calculator.calculateRouteAsSet(Ashton, Bury, journeyRequest);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldHaveAshtonToAltrincham() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,5), false,
                3, maxJourneyDuration, 5, modes);

        Set<Journey> journeys = calculator.calculateRouteAsSet(Ashton, Altrincham, journeyRequest);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldHaveAshtonToEccles() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,5), false,
                4, maxJourneyDuration, 5, modes);

        Set<Journey> journeys = calculator.calculateRouteAsSet(Ashton, Eccles, journeyRequest);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldHaveAltrinchamToAshton() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,5), false,
                3, maxJourneyDuration, 5, modes);

        Set<Journey> journeys = calculator.calculateRouteAsSet(Ashton, Altrincham, journeyRequest);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldHaveNewIslingtonToPiccGardens() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,5), false,
                3, maxJourneyDuration, 5, modes);

        Set<Journey> journeys = calculator.calculateRouteAsSet(NewIslington, PiccadillyGardens, journeyRequest);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldHaveBroadywayToPiccGardens() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,5), false,
                3, maxJourneyDuration, 5, modes);

        Set<Journey> journeys = calculator.calculateRouteAsSet(Broadway, PiccadillyGardens, journeyRequest);
        assertFalse(journeys.isEmpty());
    }

    @Disabled("Seems last tram from st peters is earlier now")
    @PiccGardens2022
    @Test
    void shouldReproIssueWithLateNightCentralTram() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(0,0), false, 3,
                maxJourneyDuration, 5, Collections.emptySet());

        Set<Journey> journeys = calculator.calculateRouteAsSet(StPetersSquare, MarketStreet, journeyRequest);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldCentralStationsInterconnect() {
        List<TramStations> central = Arrays.asList(Deansgate, StPetersSquare, Piccadilly, MarketStreet,Shudehill, ExchangeSquare, Victoria);

        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,5), false,
                3, maxJourneyDuration, 5, modes);

        for (TramStations start : central) {
            for(TramStations dest : central) {
                if (!start.getId().equals(dest.getId())) {
                    Set<Journey> journeys = calculator.calculateRouteAsSet(start.from(stationRepository), dest.from(stationRepository), journeyRequest);
                    assertFalse(journeys.isEmpty(), start + " to " + dest);
                }
            }
        }
    }
}

package com.tramchester.integration.graph.railAndTram;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.filters.ConfigurableGraphFilter;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.testSupport.RailAndTramGreaterManchesterConfig;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.GMTest;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.tramchester.domain.reference.TransportMode.*;
import static com.tramchester.testSupport.reference.TramStations.Eccles;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

@GMTest
public class RailAndTramRouteCalculatorSubGraphRoutesTest {
    private static final int TXN_TIMEOUT = 5*60;
    private static StationRepository stationRepository;
    private static RailAndTramGreaterManchesterConfig config;

    private static final TramDate when = TestEnv.testDay();

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;

    private Transaction txn;
    private RouteCalculatorTestFacade testFacade;

    private Duration maxDurationFromConfig;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new RailAndTramGreaterManchesterConfig();
        componentContainer = new ComponentsBuilder().
                configureGraphFilter(RailAndTramRouteCalculatorSubGraphRoutesTest::configureFilter).
                create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        stationRepository = componentContainer.get(StationRepository.class);
        database = componentContainer.get(GraphDatabase.class);
    }

    private static void configureFilter(ConfigurableGraphFilter graphFilter, TransportData transportData) {
        TramRouteHelper tramRouteHelper = new TramRouteHelper(transportData);
        Route fromRochdale = tramRouteHelper.getOneRoute(KnownTramRoute.RochdaleShawandCromptonManchesterEastDidisbury, when);
        Route fromAshton = tramRouteHelper.getOneRoute(KnownTramRoute.AshtonUnderLyneManchesterEccles, when);
        graphFilter.addRoute(fromAshton.getId());
        graphFilter.addRoute(fromRochdale.getId());
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

        maxDurationFromConfig = Duration.ofMinutes(config.getMaxJourneyDuration());
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
    void shouldHaveAshtonToEcclesDirect() {
        // this works fine when only tram data loaded, but fails when tram and train is loaded
        TramTime time = TramTime.of(9,0);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, 0, maxDurationFromConfig,
                1, TramsOnly);

        Set<Journey> journeys = testFacade.calculateRouteAsSet(TramStations.Ashton, Eccles, journeyRequest);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldHaveRochdaleToEastDidsDirect() {
        // this works fine when only tram data loaded, but fails when tram and train is loaded
        TramTime time = TramTime.of(9,0);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, 0, maxDurationFromConfig,
                1, TramsOnly);

        Set<Journey> journeys = testFacade.calculateRouteAsSet(Rochdale, EastDidsbury, journeyRequest);
        assertFalse(journeys.isEmpty());
    }
}

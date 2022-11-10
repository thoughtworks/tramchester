package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.*;
import com.tramchester.domain.collections.IndexedBitSet;
import com.tramchester.domain.collections.SimpleList;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.BetweenRoutesCostRepository;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.graph.search.routes.RouteCostMatrix;
import com.tramchester.graph.search.routes.RouteIndex;
import com.tramchester.graph.search.routes.RouteIndexPair;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.testTags.WorkaroundsNov2022;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.time.Duration;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.reference.KnownTramRoute.CornbrookTheTraffordCentre;
import static com.tramchester.testSupport.reference.KnownTramRoute.PiccadillyBury;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;


@Disabled("WIP")
public class RouteCalculatorRedLineIssuesTest {

    // Note this needs to be > time for whole test fixture, see note below in @After
    private static final int TXN_TIMEOUT = 5*60;

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static IntegrationTramTestConfig config;

    private RouteCalculatorTestFacade calculator;
    private final TramDate when = TestEnv.testDay();
    private Transaction txn;
    private Duration maxJourneyDuration;
    private TramRouteHelper routeHelper;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
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
        stationRepository = componentContainer.get(StationRepository.class);
        calculator = new RouteCalculatorTestFacade(componentContainer.get(RouteCalculator.class), stationRepository, txn);
        maxJourneyDuration = Duration.ofMinutes(config.getMaxJourneyDuration());

        RouteRepository routeRepository = componentContainer.get(RouteRepository.class);
        routeHelper = new TramRouteHelper(routeRepository);
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @WorkaroundsNov2022
    @Test
    void shouldReproIssueWithExchangeSquareToTraffordCenter() {

        TramDate testDate = TramDate.of(2022,11,14);

        JourneyRequest request = new JourneyRequest(testDate, TramTime.of(8,5), false, 4,
                maxJourneyDuration, 1, Collections.emptySet());

        Set<Journey> journeys = calculator.calculateRouteAsSet(Etihad, TraffordCentre, request);
        assertFalse(journeys.isEmpty());
    }

    @WorkaroundsNov2022
    @Test
    void shouldReproIssueWithAshtonToTraffordCenter() {

        TramDate testDate = TramDate.of(2022,11,14);

        JourneyRequest request = new JourneyRequest(testDate, TramTime.of(8,5), false, 4,
                maxJourneyDuration, 1, Collections.emptySet());

        Set<Journey> journeys = calculator.calculateRouteAsSet(Ashton, TraffordCentre, request);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldReproduceIssueBetweenPiccAndTraffordLine() {
        RouteIndex routeIndex = componentContainer.get(RouteIndex.class);
        RouteCostMatrix routeMatrix = componentContainer.get(RouteCostMatrix.class);

        TramDate testDate = TramDate.of(2022,11,14);

        Route routeA = routeHelper.getOneRoute(PiccadillyBury, testDate);
        Route routeB = routeHelper.getOneRoute(CornbrookTheTraffordCentre, testDate);

        RouteIndexPair indexPair = routeIndex.getPairFor(new RoutePair(routeA, routeB));

        IndexedBitSet dateOverlaps = routeMatrix.createOverlapMatrixFor(testDate);

        assertEquals(196, dateOverlaps.numberOfBitsSet());

        List<SimpleList<RouteIndexPair>> results = routeMatrix.getChangesFor(indexPair, dateOverlaps).collect(Collectors.toList());

        assertEquals(1, results.size());
    }

    @Test
    void shouldReproduceIssueBetweenAshtonAndTraffordCenter() {
        TramDate testDate = TramDate.of(2022,11, 14);

        BetweenRoutesCostRepository routesCostRepository = componentContainer.get(BetweenRoutesCostRepository.class);

        TimeRange timeRange = TimeRange.of(TramTime.of(8,5), TramTime.of(10,9));

        Station ashton = Ashton.from(stationRepository);
        Station traffordCentre = TraffordCentre.from(stationRepository);

        Set<TransportMode> modes = EnumSet.of(TransportMode.Tram);

        NumberOfChanges changes = routesCostRepository.getNumberOfChanges(ashton, traffordCentre, modes, testDate, timeRange);

        assertEquals(2, changes.getMin(), changes.toString());
    }


}
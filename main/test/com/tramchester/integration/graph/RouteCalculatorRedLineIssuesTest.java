package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.*;
import com.tramchester.domain.collections.IndexedBitSet;
import com.tramchester.domain.collections.SimpleList;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.BetweenRoutesCostRepository;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.graph.search.routes.RouteCostMatrix;
import com.tramchester.graph.search.routes.RouteIndex;
import com.tramchester.graph.search.routes.RouteIndexPair;
import com.tramchester.integration.testSupport.RouteCalculationCombinations;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.*;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.testSupport.testTags.WorkaroundsNov2022;
import org.apache.commons.collections4.SetUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.tramchester.testSupport.reference.KnownTramRoute.CornbrookTheTraffordCentre;
import static com.tramchester.testSupport.reference.KnownTramRoute.PiccadillyBury;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;


@Disabled
public class RouteCalculatorRedLineIssuesTest {

    // Note this needs to be > time for whole test fixture, see note below in @After
    private static final int TXN_TIMEOUT = 5*60;

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static IntegrationTramTestConfig config;

    private RouteCalculatorTestFacade calculator;
    private Transaction txn;
    private Duration maxJourneyDuration;
    private TramRouteHelper routeHelper;
    private StationRepository stationRepository;
    private BetweenRoutesCostRepository routesCostRepository;
    private InterchangeRepository interchangeRepository;

    public static final TramDate PROBLEM_DATE = TramDate.of(2022, 11, 21);
    public static final TramDate NORMAL_DATE = PROBLEM_DATE.plusDays(1);


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

        routesCostRepository = componentContainer.get(BetweenRoutesCostRepository.class);

        interchangeRepository = componentContainer.get(InterchangeRepository.class);


    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @WorkaroundsNov2022
    @Test
    void shouldReproIssueWithExchangeSquareToTraffordCenter() {

        TramDate testDate = PROBLEM_DATE;

        JourneyRequest request = new JourneyRequest(testDate, TramTime.of(8,5), false, 4,
                maxJourneyDuration, 1, Collections.emptySet());

        Set<Journey> journeys = calculator.calculateRouteAsSet(Etihad, TraffordCentre, request);
        assertFalse(journeys.isEmpty());
    }

    @WorkaroundsNov2022
    @Test
    void shouldReproIssueWithAshtonToTraffordCenter() {

        JourneyRequest request = new JourneyRequest(PROBLEM_DATE, TramTime.of(8,5), false, 4,
                maxJourneyDuration, 1, Collections.emptySet());

        Set<Journey> journeys = calculator.calculateRouteAsSet(Ashton, TraffordCentre, request);
        assertFalse(journeys.isEmpty());
    }

    @WorkaroundsNov2022
    @Test
    void shouldReproIssueWithAshtonToPiccadilly() {

        JourneyRequest request = new JourneyRequest(PROBLEM_DATE, TramTime.of(8,5), false, 4,
                maxJourneyDuration, 1, Collections.emptySet());

        Set<Journey> journeys = calculator.calculateRouteAsSet(Ashton, Piccadilly, request);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldHaveRouteAvailabilityAtAshton() {

        StationAvailabilityRepository availabilityRepository = componentContainer.get(StationAvailabilityRepository.class);

        Station ashton = Ashton.from(stationRepository);

        TimeRange timeRange = TimeRange.of(TramTime.of(7,0), TramTime.of(23,0));

        Set<Route> pickups = availabilityRepository.getPickupRoutesFor(ashton, PROBLEM_DATE, timeRange);

        assertFalse(pickups.isEmpty());

    }

    @Test
    void shouldCheckRangeOfDateForIssueCornbrookTraffordCentre() {

        List<TramDate> missing = getRangeOfDates(Cornbrook.getId(), TraffordCentre.getId());

        assertTrue(missing.isEmpty(), missing.toString());
    }

    @Test
    void shouldCheckRangeOfDateForIssueAshtonToTraffordCentre() {
        List<TramDate> missing = getRangeOfDates(Ashton.getId(), TraffordCentre.getId());
        assertTrue(missing.isEmpty(), missing.toString());
    }

    @Test
    void shouldCheckRangeOfDateForIssueAshtonToPom() {
        List<TramDate> missing = getRangeOfDates(Ashton.getId(), Pomona.getId());
        assertTrue(missing.isEmpty(), missing.toString());
    }

    @Test
    void shouldCheckRangeOfDateForIssueCornbrookToPom() {
        List<TramDate> missing = getRangeOfDates(Cornbrook.getId(), Pomona.getId());
        assertTrue(missing.isEmpty(), missing.toString());
    }

    @Test
    void shouldCheckRangeOfDateForIssueAshtonToCornbrook() {
        List<TramDate> missing = getRangeOfDates(Cornbrook.getId(), TraffordCentre.getId());
        assertTrue(missing.isEmpty(), missing.toString());
    }

    @NotNull
    private List<TramDate> getRangeOfDates(IdFor<Station> start, IdFor<Station> dest) {
        ProvidesLocalNow providesNow = componentContainer.get(ProvidesLocalNow.class);
        TramDate baseDate = TramDate.from(providesNow.getDateTime());

        List<JourneyRequest> requests = IntStream.range(0, 31).sorted().boxed().
                map(baseDate::plusDays).
                map(date -> new JourneyRequest(date, TramTime.of(8, 5), false, 4,
                        maxJourneyDuration, 1, Collections.emptySet())).
                collect(Collectors.toList());

        RouteCalculationCombinations routeCalculationCombinations = new RouteCalculationCombinations(componentContainer);

        Map<JourneyRequest, RouteCalculationCombinations.JourneyOrNot> results = routeCalculationCombinations.checkIfJourney(start, dest, requests);

        return results.entrySet().stream().
                filter(pair -> pair.getValue().missing()).
                map(pair -> pair.getKey().getDate().getDate()).
                sorted().
                collect(Collectors.toList());
    }

    @Test
    void shouldReproduceIssueBetweenPiccAndTraffordLine() {
        RouteIndex routeIndex = componentContainer.get(RouteIndex.class);
        RouteCostMatrix routeMatrix = componentContainer.get(RouteCostMatrix.class);

        TramDate testDate = PROBLEM_DATE;

        Route routeA = routeHelper.getOneRoute(PiccadillyBury, testDate);
        Route routeB = routeHelper.getOneRoute(CornbrookTheTraffordCentre, testDate);

        RouteIndexPair indexPair = routeIndex.getPairFor(new RoutePair(routeA, routeB));

        IndexedBitSet dateOverlaps = routeMatrix.createOverlapMatrixFor(testDate);

        assertEquals(169, dateOverlaps.numberOfBitsSet());

        List<SimpleList<RouteIndexPair>> results = routeMatrix.getChangesFor(indexPair, dateOverlaps).collect(Collectors.toList());

        assertEquals(1, results.size());
    }

    @Test
    void shouldHaveSameRouteAvailabilityForInterchanges() {

        final Set<InterchangeStation> interchanges = interchangeRepository.getAllInterchanges();

        TramDate normalDate = NORMAL_DATE;
        TramDate problemDate = PROBLEM_DATE;

        IdSet<Station> dropoffMismatch = new IdSet<>();
        IdSet<Station> pickupMismatchs = new IdSet<>();

        interchanges.forEach(interchange -> {
            final Set<Route> dropOffAtInterchange = interchange.getDropoffRoutes();
            final Set<Route> pickupAtInterchange = interchange.getPickupRoutes();

            boolean mismatchDropOffs = dropOffAtInterchange.stream().
                    anyMatch(dropOffs -> dropOffs.isAvailableOn(normalDate) != dropOffs.isAvailableOn(problemDate));

            if (mismatchDropOffs) {
                dropoffMismatch.add(interchange.getStationId());
            }

            boolean mismatchPickups = pickupAtInterchange.stream().
                    anyMatch(pickUps -> pickUps.isAvailableOn(normalDate) != pickUps.isAvailableOn(problemDate));

            if (mismatchPickups) {
                pickupMismatchs.add(interchange.getStationId());
            }

        });

        assertTrue(dropoffMismatch.isEmpty(), dropoffMismatch.toString());
        assertTrue(pickupMismatchs.isEmpty(), pickupMismatchs.toString());
    }


    @Test
    void shouldFindMismatchPickups() {
        final Set<InterchangeStation> interchanges = interchangeRepository.getAllInterchanges();

        Map<IdFor<Station>, Set<String>> results = new HashMap<>();

        interchanges.forEach(interchange -> {
            final Set<String> diffs = getDiffPickupsFor(interchange);
            if (!diffs.isEmpty()) {
                results.put(interchange.getStationId(), diffs);
            }
        });

        assertTrue(results.isEmpty(), results.toString());

    }

    @Test
    void shouldHaveSamePickupRoutesAtCornbrookInterchange() {
        Station cornbrook = Cornbrook.from(stationRepository);
        InterchangeStation interchange = interchangeRepository.getInterchange(cornbrook);

        Set<String> diff = getDiffPickupsFor(interchange);

        assertTrue(diff.isEmpty(), diff.toString() );

    }

    @NotNull
    private Set<String> getDiffPickupsFor(InterchangeStation interchangeStation) {

        TramDate normalDate = NORMAL_DATE;
        TramDate problemDate = PROBLEM_DATE;

        Set<Route> pickups = interchangeStation.getPickupRoutes();

        Set<String> problem = getRoutesNamesFor(problemDate, pickups);
        Set<String> normal = getRoutesNamesFor(normalDate, pickups);

        assertFalse(problem.isEmpty());
        assertFalse(normal.isEmpty());

        Set<String> diff = SetUtils.difference(normal, problem);
        return diff;
    }

    @Test
    void shouldHaveSameDropoffRoutesAtCornbrookInterchange() {
        Station cornbrook = Cornbrook.from(stationRepository);
        InterchangeStation interchange = interchangeRepository.getInterchange(cornbrook);

        TramDate normalDate = PROBLEM_DATE.plusDays(1);
        TramDate problemDate = PROBLEM_DATE;

        Set<Route> dropOffs = interchange.getDropoffRoutes();

        Set<String> problem = getRoutesNamesFor(problemDate, dropOffs);
        Set<String> normal = getRoutesNamesFor(normalDate, dropOffs);

        assertFalse(problem.isEmpty());
        assertFalse(normal.isEmpty());

        Set<String> diff = SetUtils.difference(normal, problem);

        assertTrue(diff.isEmpty(), diff + " problem " + problem + " normal " + normal);
    }

    @Test
    void shouldHaveExpectedConnectivityAtCornbrookProblemDate() {
        fail("todo");
    }

    @NotNull
    private Set<String> getRoutesNamesFor(TramDate problemDate, Set<Route> pickups) {
        return pickups.stream().filter(route -> route.isAvailableOn(problemDate)).map(Route::getName).collect(Collectors.toSet());
    }

    @Test
    void shouldReproduceIssueBetweenAshtonAndTraffordCenter() {
        TramDate testDate = PROBLEM_DATE;

        TimeRange timeRange = TimeRange.of(TramTime.of(8,5), TramTime.of(10,9));

        Station ashton = Ashton.from(stationRepository);
        Station traffordCentre = TraffordCentre.from(stationRepository);

        Set<TransportMode> modes = EnumSet.of(TransportMode.Tram);

        NumberOfChanges changes = routesCostRepository.getNumberOfChanges(ashton, traffordCentre, modes, testDate, timeRange);

        assertEquals(2, changes.getMin(), changes.toString());
    }

    @Test
    void shouldReproduceIssueBetweenAshtonAndTraffordCenterOtherDate() {

        TramDate testDate = NORMAL_DATE;

        TimeRange timeRange = TimeRange.of(TramTime.of(8,5), TramTime.of(10,9));

        Station ashton = Ashton.from(stationRepository);
        Station traffordCentre = TraffordCentre.from(stationRepository);

        Set<TransportMode> modes = EnumSet.of(TransportMode.Tram);

        NumberOfChanges changes = routesCostRepository.getNumberOfChanges(ashton, traffordCentre, modes, testDate, timeRange);

        assertEquals(2, changes.getMin(), changes.toString());
    }

    @Test
    void shouldHaveExpectedCallingStationsForBusReplacementRoute() {
        TramDate testDate = PROBLEM_DATE;

        Route replacementRoute = routeHelper.getOneRoute(KnownTramRoute.ReplacementRouteDeansgatePiccadilly, testDate);

        assertNotNull(replacementRoute);

        Set<Service> services = replacementRoute.getServices();

        assertFalse(services.isEmpty());

        TripRepository tripRepository = componentContainer.get(TripRepository.class);

        Set<Trip> callingTrips = tripRepository.getTrips().stream().
                filter(trip -> services.contains(trip.getService())).collect(Collectors.toSet());

        assertFalse(callingTrips.isEmpty());

        Set<Station> stationsOnRoute = callingTrips.stream().
                flatMap(trip -> trip.getStopCalls().stream()).
                map(StopCall::getStation).
                collect(Collectors.toSet());

        assertFalse(stationsOnRoute.isEmpty());

        assertEquals(4, stationsOnRoute.size(), HasId.asIds(stationsOnRoute));
    }


}
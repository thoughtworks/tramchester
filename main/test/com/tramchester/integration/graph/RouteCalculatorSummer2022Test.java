package com.tramchester.integration.graph;

import com.google.common.collect.Sets;
import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.NumberOfChanges;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.graph.search.routes.RouteToRouteCosts;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationAvailabilityRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.testSupport.testTags.Summer2022;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.reference.KnownTramRoute.*;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

@Summer2022
class RouteCalculatorSummer2022Test {

    private static final int TXN_TIMEOUT = 5*60;

    private static ComponentContainer componentContainer;
    private static TramchesterConfig testConfig;
    private static GraphDatabase database;

    private TramDate when;
    private final TramDate endOfWorks = TramDate.of(2022, 10, 21);

    private Duration maxJourneyDuration;
    private RouteCalculatorTestFacade calculator;
    private Transaction txn;
    private StationRepository stationRepository;
    private RouteRepository routeRepository;
    private RouteToRouteCosts routeToRouteCosts;
    private TramRouteHelper tramRouteHelper;
    private StationAvailabilityRepository availabilityRepository;

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

        txn = database.beginTx(TXN_TIMEOUT, TimeUnit.SECONDS);

        maxJourneyDuration = Duration.ofMinutes(testConfig.getMaxJourneyDuration());
        stationRepository = componentContainer.get(StationRepository.class);

        calculator = new RouteCalculatorTestFacade(componentContainer.get(RouteCalculator.class), stationRepository, txn);

        routeRepository = componentContainer.get(RouteRepository.class);
        routeToRouteCosts = componentContainer.get(RouteToRouteCosts.class);
        availabilityRepository = componentContainer.get(StationAvailabilityRepository.class);

        tramRouteHelper = new TramRouteHelper(routeRepository);

        when = TestEnv.testTramDay();


    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldReproIssueDuringSummer2022ClosureEccToVictoria() {

        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,5), false,
                3, maxJourneyDuration, 5, Collections.emptySet());

        Set<Journey> journeys = calculator.calculateRouteAsSet(Eccles, Victoria, journeyRequest);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldDuringSummer2022ClosureHarbourCityToCornbrook() {

        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(9,5), false,
                3, maxJourneyDuration, 5, Collections.emptySet());

        Set<Journey> journeys = calculator.calculateRouteAsSet(HarbourCity, Cornbrook, journeyRequest);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldReproIssueDuringSummer2022TraffordBar() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,5), false,
                3, maxJourneyDuration, 5, Collections.emptySet());

        Set<Journey> journeys = calculator.calculateRouteAsSet(Eccles, TraffordBar, journeyRequest);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldGetExpectedChangesBetweenEcclesAndTraffordBar() {
        TimeRange timeRange = TimeRange.of(TramTime.of(8,5), TramTime.of(10,35));

        NumberOfChanges results = routeToRouteCosts.getNumberOfChanges(Eccles.from(stationRepository), TraffordBar.from(stationRepository),
                Collections.emptySet(), when, timeRange);

        assertEquals(2, results.getMax(), results.toString());
    }

    @Test
    void shouldGetRouteCostsOnDate() {

        TimeRange timeRange = TimeRange.of(TramTime.of(8,5), TramTime.of(10,35));

        Set<TransportMode> modes = Collections.singleton(TransportMode.Tram);
        NumberOfChanges costs = routeToRouteCosts.getNumberOfChanges(Eccles.from(stationRepository),
                TraffordBar.from(stationRepository), modes, when, timeRange);

        assertEquals(2, costs.getMin(), costs.toString());
        assertEquals(2, costs.getMax(), costs.toString());
    }

    @Test
    void shouldHaveExpectedStopsForRoutes() {
        TimeRange timeRange = TimeRange.of(TramTime.of(8,5), TramTime.of(10,35));

        Station eccles = Eccles.from(stationRepository);

        Set<Trip> notCallingAtEccles = availabilityRepository.getPickupRoutesFor(eccles, when, timeRange).stream().
                flatMap(route -> route.getTrips().stream()).
                filter(trip -> !trip.getStopCalls().callsAt(eccles)).collect(Collectors.toSet());

        assertTrue(notCallingAtEccles.isEmpty(), notCallingAtEccles.toString());
    }

    @Disabled("Time table says replacement bus calls here, web says it doesn't......")
    @Test
    void shouldExpectedRoutesAtHarbourCity() {
        TimeRange timeRange = TimeRange.of(TramTime.of(8,5), TramTime.of(10,35));

        Station harbourCity = HarbourCity.from(stationRepository);

        Route replacement = tramRouteHelper.getOneRoute(ReplacementRouteFromEccles, when.plusDays(1));
                //routeRepository.getRouteById(StringIdFor.createId("METLML1:O:2022-07-16"));

        Set<Route> pickUps = availabilityRepository.getPickupRoutesFor(harbourCity, when, timeRange);
        assertFalse(pickUps.contains(replacement), harbourCity.toString());
    }

    @Test
    void shouldHaveExpectedRoutesAtTraffordBar() {
        TimeRange timeRange = TimeRange.of(TramTime.of(8,5), TramTime.of(10,35));

        Station traffordBar = TraffordBar.from(stationRepository);

        Route ashToEccles = routeRepository.getRouteById(StringIdFor.createId("METLBLUE:I:2022-07-16"));

        Set<Route> dropOffs = availabilityRepository.getDropoffRoutesFor(traffordBar, when, timeRange);
        assertFalse(dropOffs.contains(ashToEccles), traffordBar.toString());

    }

    @Test
    void shouldHaveExpectedRoutesAtEcclesDuringWorks() {
        TimeRange timeRange = TimeRange.of(TramTime.of(8,5), TramTime.of(10,35));

        Station eccles = Eccles.from(stationRepository);

        Route ecclesToAsh = tramRouteHelper.getOneRoute(EcclesManchesterAshtonUnderLyne, when);

        Set<Route> pickups = availabilityRepository.getPickupRoutesFor(eccles, when,timeRange);

        assertFalse(pickups.contains(ecclesToAsh), eccles.toString());
    }

    @Disabled("Not in the data yet...")
    @Test
    void shouldHaveExpectedRoutesAtEcclesAfterWorks() {
        TimeRange timeRange = TimeRange.of(TramTime.of(8,5), TramTime.of(10,35));

        Station eccles = Eccles.from(stationRepository);

        Route ecclesToAsh = tramRouteHelper.getOneRoute(EcclesManchesterAshtonUnderLyne, endOfWorks);

        Set<Route> pickups = availabilityRepository.getPickupRoutesFor(eccles, endOfWorks.plusDays(1),timeRange);

        assertTrue(pickups.contains(ecclesToAsh), eccles.toString());
    }

    @Test
    void shouldHaveExpectedRoutesAtEcclesAfterStartOfWorks() {
        TimeRange timeRange = TimeRange.of(TramTime.of(8,5), TramTime.of(10,35));

        Station eccles = Eccles.from(stationRepository);

        Route ecclesToAsh = tramRouteHelper.getOneRoute(KnownTramRoute.EcclesManchesterAshtonUnderLyne, when);

        Set<Route> pickups = availabilityRepository.getPickupRoutesFor(eccles, when.plusDays(2), timeRange);

        // should not be present during closure?
        assertFalse(pickups.contains(ecclesToAsh), eccles.toString());
    }

    @Test
    void shouldHaveExpectedRouteToRouteCostsTramToBus() {

        Route originalRoute = tramRouteHelper.getOneRoute(KnownTramRoute.AshtonUnderLyneManchesterEccles, when);

        Route replacementRoute = tramRouteHelper.getOneRoute(KnownTramRoute.ReplacementRouteToEccles, when);

        TimeRange timeRange = TimeRange.of(TramTime.of(8,15), TramTime.of(22,35));

        NumberOfChanges numberOfChanges = routeToRouteCosts.getNumberOfChanges(originalRoute, replacementRoute, when.plusDays(1), timeRange);

        assertEquals(1, numberOfChanges.getMin());
    }

    @Test
    void shouldHaveExpectedRouteToRouteCostsBusToTram() {

        Route originalRoute = tramRouteHelper.getOneRoute(KnownTramRoute.EcclesManchesterAshtonUnderLyne, when);

        Route replacementRoute = tramRouteHelper.getOneRoute(ReplacementRouteFromEccles, when);

        TimeRange timeRange = TimeRange.of(TramTime.of(8,15), TramTime.of(22,35));

        NumberOfChanges count = routeToRouteCosts.getNumberOfChanges(originalRoute, replacementRoute, when.plusDays(1), timeRange);

        assertEquals(1, count.getMin());
    }

    @Test
    void shouldHaveCorrectDatesForRouteOverClosureFromEccles() {
        TimeRange timeRange = TimeRange.of(TramTime.of(8,5), TramTime.of(10,35));

        Route replacementFromEccles = tramRouteHelper.getOneRoute(ReplacementRouteFromEccles, when);

        Station eccles = Eccles.from(stationRepository);

        assertTrue(availabilityRepository.isAvailable(eccles, when, timeRange));

        TramDate today = TestEnv.testDay();

        Set<Route> pickups = availabilityRepository.getPickupRoutesFor(eccles, today, timeRange);
        assertEquals(1, pickups.size());
        assertTrue(pickups.contains(replacementFromEccles));

        TramDate endOfWorks = TramDate.of(2022, 10, 20);

        Set<Route> octoberPickups = availabilityRepository.getPickupRoutesFor(eccles, endOfWorks, timeRange);
        assertEquals(1, octoberPickups.size());
        assertTrue(octoberPickups.contains(replacementFromEccles));

    }

    @Test
    void shouldHaveCorrectDatesForRouteOverClosureToEccles() {
        TimeRange timeRange = TimeRange.of(TramTime.of(8,5), TramTime.of(10,35));

        Route replacementFromEccles = tramRouteHelper.getOneRoute(ReplacementRouteToEccles, when);

        Station eccles = Eccles.from(stationRepository);

        assertTrue(availabilityRepository.isAvailable(eccles, when, timeRange));

        TramDate today = TestEnv.testDay();

        Set<Route> dropOffs = availabilityRepository.getDropoffRoutesFor(eccles, today, timeRange);
        assertEquals(1, dropOffs.size());
        assertTrue(dropOffs.contains(replacementFromEccles));

        Set<Route> octoberDropOffs = availabilityRepository.getDropoffRoutesFor(eccles, endOfWorks, timeRange);
        assertEquals(1, octoberDropOffs.size());
        assertTrue(octoberDropOffs.contains(replacementFromEccles));

    }

    @Summer2022
    @Test
    void shouldHaveHarbourCityInterchangesForReplacementRoutes() {
        InterchangeRepository interchangeRepository = componentContainer.get(InterchangeRepository.class);

        Set<InterchangeStation> all = interchangeRepository.getAllInterchanges();

        Route fromEccles = tramRouteHelper.getOneRoute(ReplacementRouteFromEccles, when);

        Set<IdFor<Station>> fromEcclesReplacementInterchanges = all.stream().
                filter(station -> station.getDropoffRoutes().contains(fromEccles)).
                map(InterchangeStation::getStationId).
                collect(Collectors.toSet());

        assertFalse(fromEcclesReplacementInterchanges.isEmpty(), fromEcclesReplacementInterchanges.toString());

        Route normalEccles = tramRouteHelper.getOneRoute(EcclesManchesterAshtonUnderLyne, when);

        Set<IdFor<Station>> normalEcclesInterchanges = all.stream().
                filter(station -> station.getPickupRoutes().contains(normalEccles)).
                map(InterchangeStation::getStationId).
                collect(Collectors.toSet());

        assertFalse(normalEcclesInterchanges.isEmpty(), normalEccles.toString());

        Sets.SetView<IdFor<Station>> overlapInterchanges = Sets.intersection(normalEcclesInterchanges, fromEcclesReplacementInterchanges);

        assertFalse(overlapInterchanges.isEmpty(), "no overlap between " +
                normalEcclesInterchanges + " and " + fromEcclesReplacementInterchanges);

        assertTrue(overlapInterchanges.contains(HarbourCity.getId()));

    }

}

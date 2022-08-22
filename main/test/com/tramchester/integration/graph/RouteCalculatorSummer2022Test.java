package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.dates.TramDateSet;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.graph.search.routes.RouteToRouteCosts;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.ServiceRepository;
import com.tramchester.repository.StationAvailabilityRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.Summer2022;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

@Summer2022
class RouteCalculatorSummer2022Test {

    private static final int TXN_TIMEOUT = 5*60;
    public static final String ONE_DAY_SERVICE = "SID00215";
    public static final TramDate START_OF_WORKS = TramDate.of(2022, 7, 16);

    private static ComponentContainer componentContainer;
    private static TramchesterConfig testConfig;
    private static GraphDatabase database;

    private LocalDate when; // = START_OF_WORKS;

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

    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
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

        tramRouteHelper = new TramRouteHelper();

        when = TestEnv.testDay();

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

    @Disabled
    @Test
    void shouldReproIssueDuringSummer2022ClosureEccToVictoriaOnClosureDate() {

        JourneyRequest journeyRequest = new JourneyRequest(START_OF_WORKS, TramTime.of(8,5), false,
                3, maxJourneyDuration, 5, Collections.emptySet());

        Set<Journey> journeys = calculator.calculateRouteAsSet(Eccles, Victoria, journeyRequest);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldHaveTraffordCenterToWhitefieldOn17August() {
        LocalDate date = LocalDate.of(2022, 8,17);
        JourneyRequest journeyRequest = new JourneyRequest(date, TramTime.of(8,5), false,
                3, maxJourneyDuration, 5, Collections.emptySet());

        Set<Journey> journeys = calculator.calculateRouteAsSet(TraffordCentre, TramStations.Whitefield, journeyRequest);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldHaveTraffordCenterToWhitefieldOn18August() {
        LocalDate date = LocalDate.of(2022, 8,18);
        JourneyRequest journeyRequest = new JourneyRequest(date, TramTime.of(8,5), false,
                3, maxJourneyDuration, 5, Collections.emptySet());

        Set<Journey> journeys = calculator.calculateRouteAsSet(TraffordCentre, TramStations.Whitefield, journeyRequest);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldHaveTraffordCenterToWhitefieldOn19August() {
        LocalDate date = LocalDate.of(2022, 8,19);
        JourneyRequest journeyRequest = new JourneyRequest(date, TramTime.of(8,5), false,
                3, maxJourneyDuration, 5, Collections.emptySet());

        Set<Journey> journeys = calculator.calculateRouteAsSet(TraffordCentre, TramStations.Whitefield, journeyRequest);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldHaveTraffordCenterToWhitefieldOn20August() {
        LocalDate date = LocalDate.of(2022, 8,20);
        JourneyRequest journeyRequest = new JourneyRequest(date, TramTime.of(8,5), false,
                3, maxJourneyDuration, 5, Collections.emptySet());

        Set<Journey> journeys = calculator.calculateRouteAsSet(TraffordCentre, TramStations.Whitefield, journeyRequest);
        assertFalse(journeys.isEmpty());
    }

    @Disabled
    @Test
    void shouldReproIssueDuringSummer2022ClosureEccToVictoriaAfterClosureDate() {
        TramDateSet failureDates = new TramDateSet();

        for (int days = 1; days < 14; days++) {

            TramDate date = START_OF_WORKS.plusDays(days);
            JourneyRequest journeyRequest = new JourneyRequest(date, TramTime.of(8,5), false,
                    3, maxJourneyDuration, 5, Collections.emptySet());

            Set<Journey> journeys = calculator.calculateRouteAsSet(Eccles, Victoria, journeyRequest);
            if (journeys.isEmpty()) {
                failureDates.add(date);
            }
        }

        assertTrue(failureDates.isEmpty(), failureDates.toString());

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

        Route replacement = tramRouteHelper.getOneRoute(KnownTramRoute.ReplacementRouteFromEccles, routeRepository, when.plusDays(1));
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
    void shouldHaveExpectedRoutesAtEcclesStartOfWorks() {
        TimeRange timeRange = TimeRange.of(TramTime.of(8,5), TramTime.of(10,35));

        Station eccles = Eccles.from(stationRepository);

        Route ecclesToAsh = tramRouteHelper.getOneRoute(KnownTramRoute.EcclesManchesterAshtonUnderLyne, routeRepository, when);

        Set<Route> pickups = availabilityRepository.getPickupRoutesFor(eccles, when,timeRange);

        // should not be present during closure?
        assertFalse(pickups.contains(ecclesToAsh), eccles.toString());
    }


    @Test
    void shouldHaveExpectedRoutesAtEcclesAfterStartOfWorks() {
        TimeRange timeRange = TimeRange.of(TramTime.of(8,5), TramTime.of(10,35));

        Station eccles = Eccles.from(stationRepository);

        //Route ashToEccles = routeRepository.getRouteById(StringIdFor.createId("METLBLUE:O:2022-07-16"));
        Route ecclesToAsh = tramRouteHelper.getOneRoute(KnownTramRoute.EcclesManchesterAshtonUnderLyne, routeRepository, when);

        Set<Route> pickups = availabilityRepository.getPickupRoutesFor(eccles, when.plusDays(2), timeRange);

        // should not be present during closure?
        assertFalse(pickups.contains(ecclesToAsh), eccles.toString());
    }

    @Disabled("in the past")
    @Test
    void shouldHaveServiceThatOperatesOnly1Days() {
        ServiceRepository serviceRepository = componentContainer.get(ServiceRepository.class);

        IdFor<Service> serviceId = StringIdFor.createId(ONE_DAY_SERVICE);
        Service service = serviceRepository.getServiceById(serviceId);

        assertFalse(service.getCalendar().operatesOn(START_OF_WORKS.minusDays(1)));
        assertTrue(service.getCalendar().operatesOn(START_OF_WORKS));
        assertFalse(service.getCalendar().operatesOn(START_OF_WORKS.plusDays(1)));

    }

    @Test
    void shouldHaveExpectedRouteToRouteCostsTramToBus() {

        Route originalRoute = tramRouteHelper.getOneRoute(KnownTramRoute.AshtonUnderLyneManchesterEccles, routeRepository, when);

        Route replacementRoute = tramRouteHelper.getOneRoute(KnownTramRoute.ReplacementRouteToEccles, routeRepository, when);

        TimeRange timeRange = TimeRange.of(TramTime.of(8,15), TramTime.of(22,35));

        int count = routeToRouteCosts.getNumberChangesFor(originalRoute, replacementRoute, when.plusDays(1), timeRange);

        assertEquals(1, count);
    }

    @Test
    void shouldHaveExpectedRouteToRouteCostsBusToTram() {

        Route originalRoute = tramRouteHelper.getOneRoute(KnownTramRoute.EcclesManchesterAshtonUnderLyne, routeRepository, when);

        Route replacementRoute = tramRouteHelper.getOneRoute(KnownTramRoute.ReplacementRouteFromEccles, routeRepository, when);

        TimeRange timeRange = TimeRange.of(TramTime.of(8,15), TramTime.of(22,35));

        int count = routeToRouteCosts.getNumberChangesFor(originalRoute, replacementRoute, when.plusDays(1), timeRange);

        assertEquals(1, count);
    }

    @Test
    void shouldHaveCorrectDatesForRouteOverClosure() {
        TimeRange timeRange = TimeRange.of(TramTime.of(8,5), TramTime.of(10,35));

        Route replacementRoute = tramRouteHelper.getOneRoute(KnownTramRoute.ReplacementRouteFromEccles, routeRepository, when);

        Station eccles = Eccles.from(stationRepository);

        //assertTrue(eccles.servesRoutePickup(replacementRoute, when, timeRange));
        assertTrue(availabilityRepository.isAvailable(eccles, when, timeRange));

        LocalDate startOfWorks = START_OF_WORKS.toLocalDate();

        Set<Route> pickupRoutesOnStartOfWorks = availabilityRepository.getPickupRoutesFor(eccles, startOfWorks, timeRange);
        assertEquals(1, pickupRoutesOnStartOfWorks.size());
        assertTrue(pickupRoutesOnStartOfWorks.contains(replacementRoute));

        assertFalse(availabilityRepository.getPickupRoutesFor(eccles, startOfWorks.minusDays(1), timeRange).contains(replacementRoute));

        Set<Route> pickupRoutesDuringWorks = availabilityRepository.getPickupRoutesFor(eccles, startOfWorks.plusDays(1), timeRange);
        assertEquals(1, pickupRoutesDuringWorks.size(), pickupRoutesDuringWorks.toString());
        assertTrue(pickupRoutesDuringWorks.contains(replacementRoute));

    }



}

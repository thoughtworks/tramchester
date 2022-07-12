package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.graph.search.RouteToRouteCosts;
import com.tramchester.integration.testSupport.RouteCalculationCombinations;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.ServiceRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.Summer2022;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

@Summer2022
class RouteCalculatorSummer2022Test {

    private static final int TXN_TIMEOUT = 5*60;

    private static ComponentContainer componentContainer;
    private static TramchesterConfig testConfig;
    private static GraphDatabase database;

    private final LocalDate when = TestEnv.testDay().plusDays(7);

    private Duration maxJourneyDuration;
    private RouteCalculatorTestFacade calculator;
    private Transaction txn;
    private StationRepository stationRepository;

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
    void shouldReproIssueDuringSummer2022TraffordBar() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,5), false,
                3, maxJourneyDuration, 5, Collections.emptySet());

        Set<Journey> journeys = calculator.calculateRouteAsSet(Eccles, TraffordBar, journeyRequest);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldGetRouteCostsOnDate() {
        RouteToRouteCosts routeToRouteCosts = componentContainer.get(RouteToRouteCosts.class);

        Set<TransportMode> modes = Collections.singleton(TransportMode.Tram);
        NumberOfChanges costs = routeToRouteCosts.getNumberOfChanges(Eccles.from(stationRepository),
                TraffordBar.from(stationRepository), modes, when);

        assertEquals(3, costs.getMin(), costs.toString());
        assertEquals(3, costs.getMax(), costs.toString());
    }

    @Test
    void shouldHaveExpectedStopsForRoutes() {
        Station eccles = Eccles.from(stationRepository);

        Set<Trip> notCallingAtEccles = eccles.getPickupRoutes(when).stream().
                flatMap(route -> route.getTrips().stream()).
                filter(trip -> !trip.getStopCalls().callsAt(eccles)).collect(Collectors.toSet());

        assertTrue(notCallingAtEccles.isEmpty(), notCallingAtEccles.toString());

    }

    @Test
    void shouldHaveServiceThatOperatesOnly1Days() {
        ServiceRepository serviceRepository = componentContainer.get(ServiceRepository.class);

        IdFor<Service> serviceId = StringIdFor.createId("SID00215");
        Service service = serviceRepository.getServiceById(serviceId);

        assertFalse(service.getCalendar().operatesOn(LocalDate.of(2022,7,15)));
        assertTrue(service.getCalendar().operatesOn(LocalDate.of(2022,7,16)));
        assertFalse(service.getCalendar().operatesOn(LocalDate.of(2022,7,17)));

    }

    @Test
    void shouldHaveCorrectDatesForRouteOverClosure() {

        IdFor<Route> routeId = StringIdFor.createId("METLBLUE:I:2022-07-16");

        RouteRepository routeRepository = componentContainer.get(RouteRepository.class);

        Route route = routeRepository.getRouteById(routeId);

        assertFalse(route.isAvailableOn(LocalDate.of(2022,7,15)));
        assertTrue(route.isAvailableOn(LocalDate.of(2022,7,16)));
        assertFalse(route.isAvailableOn(LocalDate.of(2022,7,17)), route.toString());

    }



}

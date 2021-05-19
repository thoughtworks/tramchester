package com.tramchester.integration.graph.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.CompositeStation;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.RouteReachable;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.CompositeStationRepository;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.testTags.BusTest;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusRoutesForTesting;
import com.tramchester.testSupport.reference.BusStations;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@BusTest
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class RouteReachableBusTest {
    private static ComponentContainer componentContainer;

    private RouteReachable reachable;
    private StationRepository stationRepository;
    private RouteRepository routeRepository;
    private Set<Route> altyToWarringtonRoutes;
    private CompositeStationRepository compositeStationRepository;

    @BeforeAll
    static void onceBeforeAnyTestRuns() {
        TramchesterConfig config = new IntegrationBusTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        stationRepository = componentContainer.get(StationRepository.class);
        reachable = componentContainer.get(RouteReachable.class);
        routeRepository = componentContainer.get(RouteRepository.class);
        compositeStationRepository = componentContainer.get(CompositeStationRepository.class);
        altyToWarringtonRoutes = BusRoutesForTesting.findAltyToWarrington(routeRepository);
    }

    @Test
    void shouldHaveReachabilityAltrinchamToStockport() {
        Set<Route> routes = BusRoutesForTesting.findAltyToStockport(routeRepository);

        CompositeStation interchange = compositeStationRepository.findByName("Altrincham Interchange");

        routes.forEach(route -> {
            Set<Station> starts = interchange.getContained().stream().
                    filter(station -> station.servesRoute(route)).collect(Collectors.toSet());

            IdFor<Station> stockportStationBusStop = BusStations.StopAtStockportBusStation.getId();
            starts.forEach(start -> {
                RouteStation routeStation = stationRepository.getRouteStation(start, route);
                IdSet<Station> result = reachable.getReachableStationsOnRoute(routeStation);
                assertFalse(result.isEmpty(), start.getId().toString());
                assertTrue(result.contains(stockportStationBusStop), start.getId().toString());
            });
        });

    }

    @Test
    void shouldCorrectNotReachable() {

        altyToWarringtonRoutes.forEach(altyToWarrington -> {
            RouteStation routeStation = new RouteStation(BusStations.real(stationRepository, BusStations.StopAtStockportBusStation), altyToWarrington);

            IdSet<Station> result = reachable.getReachableStationsOnRoute(routeStation);

            assertTrue(result.isEmpty());
        });
    }

    @Test
    void shouldHaveExpectedStationsReachableFromSpecificStop() {

        CompositeStation interchange = compositeStationRepository.findByName("Altrincham Interchange");

        altyToWarringtonRoutes.forEach(altyToWarrington -> {
            Set<Station> starts = interchange.getContained().stream().
                    filter(station -> station.servesRoute(altyToWarrington)).collect(Collectors.toSet());
            assertFalse(starts.isEmpty());

            assertEquals(1, starts.size());
            Station stopAtAltrincham = starts.iterator().next();

            RouteStation routeStation = stationRepository.getRouteStation(stopAtAltrincham, altyToWarrington);

            IdSet<Station> result = reachable.getReachableStationsOnRoute(routeStation);

            assertTrue(result.size()>80);
            assertTrue(result.size()<85); // diff routes have diff calling points

        });

    }

    @Disabled("some routes do seem to be isolated")
    @Test
    void shouldNotHaveStationsWithZeroReachability() {
        Set<RouteStation> routeStations = stationRepository.getRouteStations().stream().
                        filter(routeStation -> routeStation.getTransportModes().contains(TransportMode.Bus)).
                        collect(Collectors.toSet());

        Set<Route> noInterchangeOnRoute = routeStations.stream().
                filter(routeStation -> !reachable.isInterchangeReachableOnRoute(routeStation)).
                map(RouteStation::getRoute).
                collect(Collectors.toSet());

        assertEquals(0, noInterchangeOnRoute.size());

    }

}

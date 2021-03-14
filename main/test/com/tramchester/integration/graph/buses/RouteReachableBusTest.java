package com.tramchester.integration.graph.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.RouteReachable;
import com.tramchester.integration.testSupport.IntegrationBusTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TestStation;
import com.tramchester.testSupport.TestStations;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.reference.RoutesForTesting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class RouteReachableBusTest {
    private static ComponentContainer componentContainer;

    private RouteReachable reachable;
    private StationRepository stationRepository;
    private Route altyToStockportRoute;

    @BeforeAll
    static void onceBeforeAnyTestRuns() {
        TramchesterConfig config = new IntegrationBusTestConfig();
        componentContainer = new ComponentsBuilder<>().create(config, TestEnv.NoopRegisterMetrics());
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
        stationRepository = componentContainer.get(StationRepository.class);
        altyToStockportRoute = RoutesForTesting.ALTY_TO_WARRINGTON;
    }

    @Test
    void shouldHaveCorrectReachability() {
        Route route = RoutesForTesting.ALTY_TO_STOCKPORT;
        RouteStation routeStation = new RouteStation(BusStations.real(stationRepository,
                BusStations.AltrinchamInterchange), route);

        IdFor<Station> busStationId = BusStations.StockportBusStation.getId();

        IdSet<Station> result = reachable.getReachableStations(routeStation);

        assertFalse(result.isEmpty());
        assertTrue(result.contains(busStationId));
    }

    @Test
    void shouldCorrectNotReachable() {
        RouteStation routeStation = new RouteStation(BusStations.real(stationRepository, BusStations.StockportBusStation), altyToStockportRoute);

        IdSet<Station> result = reachable.getReachableStations(routeStation);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldHaveCorrectReachabilityForWholeRoute() {
        RouteStation routeStation = new RouteStation(BusStations.real(stationRepository, BusStations.AltrinchamInterchange), altyToStockportRoute);

//        Set<RouteStation> destinations = stationRepository.getRouteStations().stream().
//                filter(station -> station.getRoute().getId().equals(route.getId())).
//                collect(Collectors.toSet());

        //long toInterchange = destinations.stream().filter(station -> reachable.isInterchangeReachable(station)).count();
        IdSet<Station> result = reachable.getReachableStations(routeStation);

        assertEquals(77, result.size());
    }


    @Test
    void shouldNotHaveStationsWithZeroReachability() {
        Set<RouteStation> busRouteStations = stationRepository.getRouteStations()
                .stream().
                        filter(routeStation -> routeStation.getTransportModes().contains(TransportMode.Bus)).
                        collect(Collectors.toSet());
        Set<RouteStation> cantReachInterchange = busRouteStations.stream().
                filter(routeStation -> !reachable.isInterchangeReachable(routeStation)).collect(Collectors.toSet());

        Set<RouteStation> cutOffRouteStations = cantReachInterchange.stream().
                filter(routeStation -> reachable.getReachableStations(routeStation).isEmpty()).
                collect(Collectors.toSet());

        assertEquals(Collections.emptySet(), cutOffRouteStations);
    }

    @Test
    void shouldHaveAdjacentRoutesCorrectly() {
        assertEquals(1, getRoutes(BusStations.StockportAtAldi, BusStations.StockportNewbridgeLane).size());
        assertEquals(0, getRoutes(BusStations.StockportNewbridgeLane, BusStations.StockportAtAldi).size());
    }

    private List<Route> getRoutes(BusStations start, BusStations neighbour) {
        return reachable.getRoutesFromStartToNeighbour(getReal(start), getReal(neighbour));
    }

    private Station getReal(TestStations stations) {
        return TestStation.real(stationRepository, stations);
    }

    private RouteStation createRouteStation(Route route, Station station) {
        return new RouteStation(station, route);
    }
}

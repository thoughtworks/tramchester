package com.tramchester.integration.graph.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.RouteReachable;
import com.tramchester.integration.testSupport.IntegrationBusTestConfig;
import com.tramchester.repository.RouteRepository;
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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteReachableBusTest {
    private static ComponentContainer componentContainer;

    private RouteReachable reachable;
    private StationRepository stationRepository;
    private Route route;
    private RouteRepository routeRepository;

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
        routeRepository = componentContainer.get(RouteRepository.class);
        route = RoutesForTesting.ALTY_TO_STOCKPORT_WBT;
    }

    @Test
    void shouldHaveCorrectReachabilityOrInterchanges() {
        RouteStation routeStation = new RouteStation(BusStations.real(stationRepository, BusStations.AltrinchamInterchange), route);

        IdFor<Station> busStationId = BusStations.StockportBusStation.getId();
        IdSet<Station> destinations = IdSet.singleton(busStationId);

        IdSet<Station> result = reachable.getRouteReachableWithInterchange(routeStation, destinations);

        assertEquals(1, result.size());
        assertTrue(result.contains(busStationId));
    }

    @Test
    void shouldCorrectNotReachable() {
        RouteStation routeStation = new RouteStation(BusStations.real(stationRepository, BusStations.StockportBusStation), route);

        IdFor<Station> busStationId = BusStations.AltrinchamInterchange.getId();
        IdSet<Station> destinations = IdSet.singleton(busStationId);

        IdSet<Station> result = reachable.getRouteReachableWithInterchange(routeStation, destinations);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldHaveCorrectReachabilityForWholeRoute() {
        RouteStation routeStation = new RouteStation(BusStations.real(stationRepository, BusStations.AltrinchamInterchange), route);

        IdSet<Station> destinations = stationRepository.getRouteStations().stream().
                filter(station -> station.getRoute().getId().equals(route.getId())).
                map(station -> station.getStation().getId()).
                collect(IdSet.idCollector());

        IdSet<Station> result = reachable.getRouteReachableWithInterchange(routeStation, destinations);

        assertEquals(82, result.size());

    }

    @Test
    void shouldHaveAdjacentRoutesCorrectly() {
        assertEquals(1, getRoutes(BusStations.StockportAldi, BusStations.StockportNewbridgeLane).size());
        assertEquals(0, getRoutes(BusStations.StockportNewbridgeLane, BusStations.StockportAldi).size());
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

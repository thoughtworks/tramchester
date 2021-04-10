package com.tramchester.integration.repository.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Route;
import com.tramchester.domain.places.CompositeStation;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.CompositeStationRepository;
import com.tramchester.repository.ReachabilityRepository;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.reference.BusRoutesForTesting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class ReachabilityRepositoryBusTest {
    private static ComponentContainer componentContainer;

    private ReachabilityRepository reachabilityRepository;
    private RouteRepository routeRepository;
    private StationRepository stationRepository;
    private CompositeStationRepository compositeStationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder().create(new IntegrationBusTestConfig(), TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        routeRepository = componentContainer.get(RouteRepository.class);
        reachabilityRepository = componentContainer.get(ReachabilityRepository.class);
        compositeStationRepository = componentContainer.get(CompositeStationRepository.class);
        stationRepository = componentContainer.get(StationRepository.class);
    }

    @Test
    void shouldHaveAltyToStockport() {
        CompositeStation interchange = compositeStationRepository.findByName("Altrincham Interchange");
        Set<Route> altyToStockportRoutes = BusRoutesForTesting.findAltyToStockport(routeRepository);

        altyToStockportRoutes.forEach(altyToStockport -> {
            Set<Station> stopsForRoutes = interchange.getContained().stream().
                    filter(station -> station.servesRoute(altyToStockport)).collect(Collectors.toSet());

            stopsForRoutes.forEach(station -> {
                RouteStation routeStation = stationRepository.getRouteStation(station, altyToStockport);
                assertTrue(reachable(routeStation, BusStations.StopAtStockportBusStation), station.getId().toString());
            });
        });

    }

    @Test
    void shouldHaveStockportRomileyCircular() {
        Set<Route> stockportMarpleRomileyCircularRoutes = BusRoutesForTesting.findStockportMarpleRomileyCircular(routeRepository);

        Station atAldi = stationRepository.getStationById(BusStations.StockportAtAldi.getId());

        Set<Route> routesCallingAtAldi = stockportMarpleRomileyCircularRoutes.stream().filter(atAldi::servesRoute).collect(Collectors.toSet());
        assertFalse(routesCallingAtAldi.isEmpty(), "no routes match");

        routesCallingAtAldi.forEach(route -> {
            RouteStation routeStation = stationRepository.getRouteStation(atAldi, route);
            assertTrue(reachable(routeStation, BusStations.StockportNewbridgeLane), "failed for " + route.getId());
        });
    }

    private boolean reachable(RouteStation routeStation, BusStations destinationStation) {
        return reachabilityRepository.stationReachable(routeStation, BusStations.of(destinationStation));
    }

}

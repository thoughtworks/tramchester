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
import org.jetbrains.annotations.NotNull;
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
        componentContainer = new ComponentsBuilder<>().create(new IntegrationBusTestConfig(), TestEnv.NoopRegisterMetrics());
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
        Route altyToStockport = BusRoutesForTesting.findAltyToStockport(routeRepository);

        Set<Station> stopsForRoutes = interchange.getContained().stream().filter(station -> station.servesRoute(altyToStockport)).collect(Collectors.toSet());

        stopsForRoutes.forEach(station -> {
            RouteStation routeStation = stationRepository.getRouteStation(station, altyToStockport);
            assertTrue(reachable(routeStation, BusStations.StopAtStockportBusStation), station.getId().toString());
        });
    }

    @Test
    void shouldHaveStockportRomileyCircular() {
        Route stockportMarpleRomileyCircular = BusRoutesForTesting.findStockportMarpleRomileyCircular(routeRepository);
        assertTrue(reachable(createRouteStation(stockportMarpleRomileyCircular, BusStations.StockportAtAldi), BusStations.StockportNewbridgeLane ));
    }

    private boolean reachable(RouteStation routeStation, BusStations destinationStation) {
        return reachabilityRepository.stationReachable(routeStation, BusStations.of(destinationStation));
    }

    @NotNull
    private RouteStation createRouteStation(Route route, BusStations station) {
        return new RouteStation(BusStations.of(station), route);
    }

}

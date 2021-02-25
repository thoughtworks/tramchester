package com.tramchester.integration.repository.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.integration.testSupport.IntegrationBusTestConfig;
import com.tramchester.integration.testSupport.IntegrationTrainTestConfig;
import com.tramchester.repository.ReachabilityRepository;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.reference.RoutesForTesting;
import com.tramchester.testSupport.reference.TrainStations;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import static com.tramchester.testSupport.reference.TrainStations.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class ReachabilityRepositoryBusTest {
    private ReachabilityRepository repository;
    private static ComponentContainer componentContainer;
    private RouteRepository routeRepo;

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
        routeRepo = componentContainer.get(RouteRepository.class);
        repository = componentContainer.get(ReachabilityRepository.class);
    }

    @Test
    void shouldCreateReachabilityMatrix() {
        Route route = routeRepo.getRouteById(RoutesForTesting.StockportMarpleRomileyCircular.getId());

        assertTrue(reachable(createRouteStation(route, BusStations.StockportAldi), BusStations.StockportNewbridgeLane ));
        // wrong direction for the route
        assertFalse(reachable(createRouteStation(route, BusStations.StockportNewbridgeLane), BusStations.StockportAldi));
    }

    private boolean reachable(RouteStation routeStation, BusStations destinationStation) {
        return repository.stationReachable(routeStation, BusStations.of(destinationStation));
    }

    @NotNull
    private RouteStation createRouteStation(Route route, BusStations station) {
        return new RouteStation(BusStations.of(station), route);
    }

}

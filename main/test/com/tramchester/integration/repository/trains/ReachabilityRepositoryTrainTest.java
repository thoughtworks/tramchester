package com.tramchester.integration.repository.trains;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.Route;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.integration.testSupport.IntegrationTrainTestConfig;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.ReachabilityRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TrainStations;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import static com.tramchester.testSupport.reference.TrainStations.ManchesterPiccadilly;
import static com.tramchester.testSupport.reference.TrainStations.Stockport;
import static org.junit.jupiter.api.Assertions.*;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class ReachabilityRepositoryTrainTest {
    private ReachabilityRepository repository;
    private static ComponentContainer componentContainer;
    private RouteRepository routeRepo;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder<>().create(new IntegrationTrainTestConfig(), TestEnv.NoopRegisterMetrics());
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
        Route route = routeRepo.getRouteById(StringIdFor.createId("7362")); // shortName='NT:CTR->MAN'

        assertNotNull(route);

        assertTrue(reachable(createRouteStation(route, Stockport), ManchesterPiccadilly));
    }

    private boolean reachable(RouteStation routeStation, TrainStations destinationStation) {
        return repository.stationReachable(routeStation, TrainStations.of(destinationStation));
    }

    @NotNull
    private RouteStation createRouteStation(Route route, TrainStations station) {
        return new RouteStation(TrainStations.of(station), route);
    }

}

package com.tramchester.integration.repository.trains;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.Route;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.IntegrationTrainTestConfig;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.ReachabilityRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TrainStations;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.reference.TrainStations.*;
import static org.junit.jupiter.api.Assertions.*;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class ReachabilityRepositoryTrainTest {
    private ReachabilityRepository repository;
    private static ComponentContainer componentContainer;
    private RouteRepository routeRepository;
    private StationRepository stationRepository;

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
        routeRepository = componentContainer.get(RouteRepository.class);
        stationRepository = componentContainer.get(StationRepository.class);
        repository = componentContainer.get(ReachabilityRepository.class);
    }

    @Test
    void shouldCreateReachabilityMatrix() {
        Route route = TestEnv.singleRoute(routeRepository, StringIdFor.createId("NT"), "NT:CTR->MAN");
        assertTrue(reachable(createRouteStation(route, Stockport), ManchesterPiccadilly));
    }

    @Test
    void shouldHaveReachabilityFromHale() {
        Station hale = stationRepository.getStationById(Hale.getId());
        Set<Route> haleRoutes = hale.getRoutes();

        Set<String> routesBetween = haleRoutes.stream().
                filter(route -> reachable(stationRepository.getRouteStation(hale, route), Knutsford)).
                map(Route::getName).
                collect(Collectors.toSet());

        assertEquals(haleRoutes.size(), routesBetween.size(), routesBetween.toString());
    }

    @Test
    void shouldHaveReachabilityFromKnutsford() {
        Station knutsford = stationRepository.getStationById(Knutsford.getId());
        Set<Route> knutsfordRoutes = knutsford.getRoutes();

        Set<String> routesBetween = knutsfordRoutes.stream().
                filter(route -> reachable(stationRepository.getRouteStation(knutsford, route), Hale)).
                map(Route::getName).
                collect(Collectors.toSet());

        assertEquals(knutsfordRoutes.size(), routesBetween.size(), routesBetween.toString());
    }

    private boolean reachable(RouteStation routeStation, TrainStations destinationStation) {
        return repository.stationReachable(routeStation, TrainStations.of(destinationStation));
    }

    @NotNull
    private RouteStation createRouteStation(Route route, TrainStations station) {
        return new RouteStation(TrainStations.of(station), route);
    }

}

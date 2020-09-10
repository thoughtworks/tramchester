package com.tramchester.integration.repository;

import com.tramchester.Dependencies;
import com.tramchester.domain.Route;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.TramReachabilityRepository;
import com.tramchester.testSupport.RoutesForTesting;
import com.tramchester.testSupport.TramStations;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

import static com.tramchester.testSupport.TramStations.*;

class TramReachabilityRepositoryTest {
    private TramReachabilityRepository repository;
    private static Dependencies dependencies;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTramTestConfig());
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        repository = dependencies.get(TramReachabilityRepository.class);
    }

    @Test
    void shouldCreateReachabilityMatrix() {

        // wrong direction
        Assertions.assertFalse(reachable(getRouteStation(NavigationRoad, RoutesForTesting.PICC_TO_ALTY), TraffordBar));
        // right direction
        Assertions.assertTrue(reachable(getRouteStation(NavigationRoad, RoutesForTesting.ALTY_TO_PICC), TraffordBar));
        // wrong direction
        Assertions.assertFalse(reachable(getRouteStation(NavigationRoad, RoutesForTesting.PICC_TO_ALTY), ManAirport));
        // right direction with interchange
        Assertions.assertTrue(reachable(getRouteStation(NavigationRoad, RoutesForTesting.ALTY_TO_PICC), ManAirport));
        // self reachable
        Assertions.assertTrue(reachable(getRouteStation(NavigationRoad, RoutesForTesting.ALTY_TO_PICC), NavigationRoad));

        // right direction
        Assertions.assertTrue(reachable(getRouteStation(RochdaleRail, RoutesForTesting.ROCH_TO_DIDS), Monsall));
        // wrong direction
        Assertions.assertFalse(reachable(getRouteStation(RochdaleRail, RoutesForTesting.DIDS_TO_ROCH), Monsall));
        // towards victoria, so find an interchange
        Assertions.assertTrue(reachable(getRouteStation(Monsall, RoutesForTesting.ROCH_TO_DIDS), RochdaleRail));
    }

    private boolean reachable(RouteStation routeStation, TramStations destinationStation) {
        return repository.stationReachable(routeStation, TramStations.of(destinationStation));
    }

    @NotNull
    private RouteStation getRouteStation(TramStations station, Route route) {
        return new RouteStation(TramStations.of(station), route);
    }

    @Test
    void shouldRepoIssueAltyToDeangates() {
        Assertions.assertTrue(reachable(getRouteStation(Altrincham, RoutesForTesting.ALTY_TO_PICC), Deansgate));
        Assertions.assertTrue(reachable(getRouteStation(StPetersSquare, RoutesForTesting.ALTY_TO_PICC), Deansgate));

        // TODO Lockdown
        // no alty to bury route
//        Assertions.assertTrue(reachable(getRouteStation(Altrincham, RoutesForTesting.ALTY_TO_BURY), Deansgate));
//        Assertions.assertTrue(reachable(getRouteStation(StPetersSquare, RoutesForTesting.ALTY_TO_BURY), Deansgate));
    }


}

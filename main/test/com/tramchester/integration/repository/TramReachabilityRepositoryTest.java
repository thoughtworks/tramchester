package com.tramchester.integration.repository;

import com.tramchester.Dependencies;
import com.tramchester.domain.Route;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.reference.KnownRoute;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.TramReachabilityRepository;
import com.tramchester.testSupport.RoutesForTesting;
import com.tramchester.testSupport.TramStations;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

import static com.tramchester.domain.reference.KnownRoute.*;
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
        Assertions.assertFalse(reachable(createRouteStation(NavigationRoad, PiccadillyAltrincham), TraffordBar));
        // right direction
        Assertions.assertTrue(reachable(createRouteStation(NavigationRoad, AltrinchamPiccadilly), TraffordBar));
        // wrong direction
        Assertions.assertFalse(reachable(createRouteStation(NavigationRoad, PiccadillyAltrincham), ManAirport));
        // right direction with interchange
        Assertions.assertTrue(reachable(createRouteStation(NavigationRoad, AltrinchamPiccadilly), ManAirport));
        // self reachable
        Assertions.assertTrue(reachable(createRouteStation(NavigationRoad, AltrinchamPiccadilly), NavigationRoad));

        // right direction
        Assertions.assertTrue(reachable(createRouteStation(RochdaleRail, RochdaleManchesterEDidsbury), Monsall));
        // wrong direction
        Assertions.assertFalse(reachable(createRouteStation(RochdaleRail, EDidsburyManchesterRochdale), Monsall));
        // towards victoria, so find an interchange
        Assertions.assertTrue(reachable(createRouteStation(Monsall, RochdaleManchesterEDidsbury), RochdaleRail));
    }
    
    @Test
    void shouldRepoIssueAltyToDeangates() {
        Assertions.assertTrue(reachable(createRouteStation(Altrincham, AltrinchamPiccadilly), Deansgate));
        Assertions.assertTrue(reachable(createRouteStation(StPetersSquare, AltrinchamPiccadilly), Deansgate));

        // TODO Lockdown
        // no alty to bury route
//        Assertions.assertTrue(reachable(getRouteStation(Altrincham, RoutesForTesting.ALTY_TO_BURY), Deansgate));
//        Assertions.assertTrue(reachable(getRouteStation(StPetersSquare, RoutesForTesting.ALTY_TO_BURY), Deansgate));
    }
    
    private boolean reachable(RouteStation routeStation, TramStations destinationStation) {
        return repository.stationReachable(routeStation, TramStations.of(destinationStation));
    }

    @NotNull
    private RouteStation createRouteStation(TramStations station, KnownRoute knownRoute) {
        Route route = RoutesForTesting.createTramRoute(knownRoute);
        return new RouteStation(TramStations.of(station), route);
    }


}

package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.RouteReachable;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestStation;
import com.tramchester.testSupport.TramStations;
import org.junit.jupiter.api.*;

import java.util.List;

import static com.tramchester.testSupport.RoutesForTesting.*;
import static com.tramchester.testSupport.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

class TramRouteReachableTest {
    private static Dependencies dependencies;

    private RouteReachable reachable;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestRuns() {
        dependencies = new Dependencies();
        TramchesterConfig config = new IntegrationTramTestConfig();
        dependencies.initialise(config);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        stationRepository = dependencies.get(StationRepository.class);
        reachable = dependencies.get(RouteReachable.class);
    }

    @Test
    void shouldHaveCorrectReachabilityOrInterchanges() {
        assertTrue(reachable(ALTY_TO_PICC, NavigationRoad, ManAirport));
        assertFalse(reachable(PICC_TO_ALTY, NavigationRoad, ManAirport));

        assertTrue(reachable(AIR_TO_VIC, ManAirport, StWerburghsRoad));
        assertFalse(reachable(VIC_TO_AIR, ManAirport, StWerburghsRoad));
    }

    @Test
    void shouldHaveCorrectReachabilityMonsalToRochs() {
        assertTrue(reachable(ROCH_TO_DIDS, RochdaleRail, Monsall));
        assertTrue(reachable(DIDS_TO_ROCH, Monsall, RochdaleRail));
    }

    @Test
    void shouldHaveAdjacentRoutesCorrectly() {

        // TODO Lockdown 2->1 for next two tests, only one route to alty now
        assertEquals(1, getRoutes(NavigationRoad, Altrincham).size());
        assertEquals(1, getRoutes(Altrincham, NavigationRoad).size());

        // 5 not the 7 on the map, only 6 routes modelled in timetable data, 1 of which does not go between these 2
        // TODO Lockdown 5->4
        assertEquals(4, getRoutes(Deansgate, StPetersSquare).size());

        assertEquals(2, getRoutes(StPetersSquare, PiccadillyGardens).size());

        // TODO Lockdown 2->1
        assertEquals(1, getRoutes(StPetersSquare, MarketStreet).size());

        assertEquals(0, getRoutes(Altrincham, Cornbrook).size());
    }

    private List<Route> getRoutes(TramStations start, TramStations neighbour) {
        return reachable.getRoutesFromStartToNeighbour(getReal(start), getReal(neighbour));
    }

    private Station getReal(TramStations stations) {
        return TestStation.real(stationRepository, stations);
    }

    private boolean reachable(Route route, TramStations routeStation, TramStations dest) {
        return reachable.getRouteReachableWithInterchange(createRouteStation(route, getReal(routeStation)), getReal(dest));
    }

    private RouteStation createRouteStation(Route route, Station station) {
        return new RouteStation(station, route);
    }
}

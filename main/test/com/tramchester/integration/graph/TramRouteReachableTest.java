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
        assertTrue(reachable(ALTY_TO_PICC, TramStations.NavigationRoad, TramStations.ManAirport));
        assertFalse(reachable(PICC_TO_ALTY, TramStations.NavigationRoad, TramStations.ManAirport));

        assertTrue(reachable(AIR_TO_VIC, TramStations.ManAirport, TramStations.StWerburghsRoad));
        assertFalse(reachable(VIC_TO_AIR, TramStations.ManAirport, TramStations.StWerburghsRoad));
    }

    @Test
    void shouldHaveCorrectReachabilityMonsalToRochs() {
        assertTrue(reachable(ROCH_TO_DIDS, TramStations.RochdaleRail, TramStations.Monsall));
        assertTrue(reachable(DIDS_TO_ROCH, TramStations.Monsall, TramStations.RochdaleRail));
    }

    @Test
    void shouldHaveAdjacentRoutesCorrectly() {

        // TODO Lockdown 2->1 for next two tests, only one route to alty now
        assertEquals(1, getRoutes(TramStations.NavigationRoad, TramStations.Altrincham).size());
        assertEquals(1, getRoutes(TramStations.Altrincham, TramStations.NavigationRoad).size());

        // 5 not the 7 on the map, only 6 routes modelled in timetable data, 1 of which does not go between these 2
        // TODO Lockdown 5->4
        assertEquals(4, getRoutes(TramStations.Deansgate, TramStations.StPetersSquare).size());

        assertEquals(2, getRoutes(TramStations.StPetersSquare, TramStations.PiccadillyGardens).size());

        // TODO Lockdown 2->1
        assertEquals(1, getRoutes(TramStations.StPetersSquare, TramStations.MarketStreet).size());

        assertEquals(0, getRoutes(TramStations.Altrincham, TramStations.Cornbrook).size());
    }

    private List<Route> getRoutes(TramStations start, TramStations neighbour) {
        return reachable.getRoutesFromStartToNeighbour(getReal(start), getReal(neighbour));
    }

    private Station getReal(TramStations stations) {
        return TestStation.real(stationRepository, stations);
    }

    private boolean reachable(Route route, TramStations routeStation, TramStations dest) {
        return reachable.getRouteReachableWithInterchange(getRouteStation(route, getReal(routeStation)), getReal(dest));
    }

    private RouteStation getRouteStation(Route route, Station station) {
        return new RouteStation(station, route);
    }
}

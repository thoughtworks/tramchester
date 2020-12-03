package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.KnownRoute;
import com.tramchester.graph.RouteReachable;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.RoutesForTesting;
import com.tramchester.testSupport.TestStation;
import com.tramchester.testSupport.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.tramchester.domain.reference.KnownRoute.*;
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
        assertTrue(reachable(AltrinchamPiccadilly, NavigationRoad, ManAirport));
        assertFalse(reachable(PiccadillyAltrincham, NavigationRoad, ManAirport));

        assertTrue(reachable(ManchesterAirportVictoria, ManAirport, StWerburghsRoad));
        assertFalse(reachable(VictoriaManchesterAirport, ManAirport, StWerburghsRoad));
    }

    @Test
    void shouldHaveCorrectReachabilityMonsalToRochs() {
        assertTrue(reachable(RochdaleManchesterEDidsbury, RochdaleRail, Monsall));
        assertTrue(reachable(EDidsburyManchesterRochdale, Monsall, RochdaleRail));
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

    private boolean reachable(KnownRoute knownRoute, TramStations routeStation, TramStations dest) {
        Route route = RoutesForTesting.createTramRoute(knownRoute);
        return reachable.getRouteReachableWithInterchange(createRouteStation(route, getReal(routeStation)), getReal(dest));
    }

    private RouteStation createRouteStation(Route route, Station station) {
        return new RouteStation(station, route);
    }
}

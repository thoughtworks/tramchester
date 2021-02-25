package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.KnownTramRoute;
import com.tramchester.graph.RouteReachable;
import com.tramchester.integration.testSupport.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.RoutesForTesting;
import com.tramchester.testSupport.TestStation;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.tramchester.domain.reference.KnownTramRoute.*;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

class RouteReachableTramTest {
    private static ComponentContainer componentContainer;

    private RouteReachable reachable;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestRuns() {
        TramchesterConfig config = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder<>().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        stationRepository = componentContainer.get(StationRepository.class);
        reachable = componentContainer.get(RouteReachable.class);
    }

    @Test
    void shouldHaveCorrectReachabilityOrInterchanges() {
        assertTrue(reachable(AltrinchamPiccadilly, NavigationRoad, ManAirport));
        assertFalse(reachable(PiccadillyAltrincham, NavigationRoad, ManAirport));

        assertTrue(reachable(ManchesterAirportVictoria, ManAirport, StWerburghsRoad));
        assertFalse(reachable(VictoriaManchesterAirport, ManAirport, StWerburghsRoad));
    }

    @Test
    void shouldCorrectNotReachable() {
        assertTrue(reachable(AltrinchamPiccadilly, NavigationRoad, OldTrafford));
        assertFalse(reachable(PiccadillyAltrincham, NavigationRoad, OldTrafford));

        assertTrue(reachable(PiccadillyAltrincham, OldTrafford, NavigationRoad));
        // Old Trafford towards Piccadilly encouters an interchance
        assertTrue(reachable(AltrinchamPiccadilly, OldTrafford, NavigationRoad));
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

    private boolean reachable(KnownTramRoute knownRoute, TramStations routeStation, TramStations dest) {
        Route route = RoutesForTesting.createTramRoute(knownRoute);
        return reachable.getRouteReachableWithInterchange(createRouteStation(route, getReal(routeStation)), getReal(dest));
    }

    private RouteStation createRouteStation(Route route, Station station) {
        return new RouteStation(station, route);
    }
}

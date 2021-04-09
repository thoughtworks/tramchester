package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.graph.RouteReachable;
import com.tramchester.integration.testSupport.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TestStation;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.tramchester.testSupport.reference.KnownTramRoute.*;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

class RouteReachableTramTest {
    private static ComponentContainer componentContainer;

    private RouteReachable reachable;
    private StationRepository stationRepository;
    private TramRouteHelper tramRouteHelper;

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
        tramRouteHelper = new TramRouteHelper(componentContainer);
    }

    @Test
    void shouldHaveCorrectReachabilityOrInterchanges() {
        assertTrue(reachableInterchange(AltrinchamPiccadilly, NavigationRoad));
        assertFalse(reachableInterchange(PiccadillyAltrincham, NavigationRoad));

        assertTrue(reachableStations(ManchesterAirportWythenshaweVictoria, ManAirport).contains(StWerburghsRoad.getId()));
        assertFalse(reachableStations(VictoriaWythenshaweManchesterAirport, ManAirport).contains(StWerburghsRoad.getId()));
    }

    @Test
    void shouldCorrectNotReachable() {
        assertTrue(reachableStations(AltrinchamPiccadilly, NavigationRoad).contains(OldTrafford.getId()));
        assertFalse(reachableStations(PiccadillyAltrincham, NavigationRoad).contains(OldTrafford.getId()));

        assertTrue(reachableStations(PiccadillyAltrincham, OldTrafford).contains(NavigationRoad.getId()));
        // Old Trafford towards Piccadilly encouters an interchance
        assertTrue(reachableInterchange(AltrinchamPiccadilly, NavigationRoad));
    }

    @Test
    void shouldHaveCorrectReachabilityMonsalToRochs() {
        assertTrue(reachableStations(RochdaleShawandCromptonManchesterEastDidisbury, RochdaleRail).contains(Monsall.getId()));
        assertTrue(reachableStations(EastDidisburyManchesterShawandCromptonRochdale, Monsall).contains(RochdaleRail.getId()));
    }

    private Station getReal(TramStations stations) {
        return TestStation.real(stationRepository, stations);
    }

    private boolean reachableInterchange(KnownTramRoute knownRoute, TramStations routeStation) {
        Route route = tramRouteHelper.get(knownRoute);
        RouteStation start = createRouteStation(route, getReal(routeStation));
        return reachable.isInterchangeReachable(start);
    }

    private IdSet<Station> reachableStations(KnownTramRoute knownRoute, TramStations routeStation) {
        Route route = tramRouteHelper.get(knownRoute);
        RouteStation start = createRouteStation(route, getReal(routeStation));
        return reachable.getReachableStations(start);
    }

    private RouteStation createRouteStation(Route route, Station station) {
        return new RouteStation(station, route);
    }
}

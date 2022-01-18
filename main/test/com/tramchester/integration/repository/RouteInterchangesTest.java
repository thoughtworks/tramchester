package com.tramchester.integration.repository;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.InterchangeStation;
import com.tramchester.domain.Route;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.RouteInterchanges;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.KnownTramRoute;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

public class RouteInterchangesTest {

    private static ComponentContainer componentContainer;
    private RouteInterchanges routeInterchanges;
    private StationRepository stationRepository;
    private TramRouteHelper tramRouteHelper;
    private InterchangeRepository interchangeRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder().create(new IntegrationTramTestConfig(), TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        stationRepository = componentContainer.get(StationRepository.class);
        RouteRepository routeRepository = componentContainer.get(RouteRepository.class);
        routeInterchanges = componentContainer.get(RouteInterchanges.class);
        tramRouteHelper = new TramRouteHelper(routeRepository);
        interchangeRepository = componentContainer.get(InterchangeRepository.class);
    }

    @Test
    void shouldGetInterchangesForARoute() {

        Set<Route> routes = tramRouteHelper.get(KnownTramRoute.BuryManchesterAltrincham);

        Set<InterchangeStation> found = routes.stream().
                flatMap(route -> routeInterchanges.getFor(route).stream()).collect(Collectors.toSet());

        assertFalse(found.isEmpty());
    }

    @Test
    void shouldGetCostToInterchangeForRouteStation() {

        Set<Route> routes = tramRouteHelper.get(KnownTramRoute.AltrinchamPiccadilly);

        List<RouteStation> navigationRoadRouteStations = stationRepository.getRouteStationsFor(NavigationRoad.getId()).stream().
                filter(routeStation -> routes.contains(routeStation.getRoute())).collect(Collectors.toList());

        assertFalse(navigationRoadRouteStations.isEmpty());

        RouteStation navigationRoad = navigationRoadRouteStations.get(0);

        int cost = routeInterchanges.costToInterchange(navigationRoad);

        // cost to trafford bar
        assertEquals(14, cost);

    }

    @Test
    void shouldGetCostToInterchangeForRouteStationAdjacent() {

        Set<Route> routes = tramRouteHelper.get(KnownTramRoute.AltrinchamPiccadilly);

        List<RouteStation> oldTraffordRouteStations = stationRepository.getRouteStationsFor(OldTrafford.getId()).stream().
                filter(routeStation -> routes.contains(routeStation.getRoute())).collect(Collectors.toList());

        assertFalse(oldTraffordRouteStations.isEmpty());

        RouteStation oldTrafford = oldTraffordRouteStations.get(0);

        int cost = routeInterchanges.costToInterchange(oldTrafford);

        // cost to trafford bar
        assertEquals(2, cost);

    }

    @Test
    void shouldGetZeroCostToInterchangeForRouteStationThatIsInterchange() {

        Set<Route> routes = tramRouteHelper.get(KnownTramRoute.AltrinchamPiccadilly);

        List<RouteStation> cornbrookRouteStations = stationRepository.getRouteStationsFor(Cornbrook.getId()).
                stream().
                filter(routeStation -> routes.contains(routeStation.getRoute())).
                collect(Collectors.toList());

        assertFalse(cornbrookRouteStations.isEmpty());

        cornbrookRouteStations.forEach(routeStation -> {
                    int cost = routeInterchanges.costToInterchange(routeStation);
                    assertEquals(0, cost);
                }
        );
    }

    @Test
    void shouldGetMaxCostIfNoInterchangeBetweenStationAndEndOfTheRoute() {
        Set<Route> routes = tramRouteHelper.get(KnownTramRoute.PiccadillyAltrincham);

        List<RouteStation> navigationRoadRouteStations = stationRepository.getRouteStationsFor(NavigationRoad.getId()).stream().
                filter(routeStation -> routes.contains(routeStation.getRoute())).collect(Collectors.toList());

        assertFalse(navigationRoadRouteStations.isEmpty());

        navigationRoadRouteStations.forEach(routeStation -> {
            int cost = routeInterchanges.costToInterchange(routeStation);
            assertEquals(Integer.MAX_VALUE, cost);
        });

    }

    @Test
    void shouldHaveConsistencyOnZeroCostToInterchangeAndInterchanges() {
        Set<RouteStation> zeroCostToInterchange = stationRepository.getRouteStations().stream().
                filter(routeStation -> routeInterchanges.costToInterchange(routeStation) == 0).
                collect(Collectors.toSet());

        Set<RouteStation> zeroCostButNotInterchange = zeroCostToInterchange.stream().
                filter(zeroCost -> !interchangeRepository.isInterchange(zeroCost.getStation())).
                collect(Collectors.toSet());

        assertTrue(zeroCostButNotInterchange.isEmpty(), zeroCostButNotInterchange.toString());
    }



}



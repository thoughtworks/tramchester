package com.tramchester.integration.repository;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.Route;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.RouteInterchangeRepository;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.Summer2022;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.TestEnv.assertMinutesEquals;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

public class RouteInterchangesTest {

    private static ComponentContainer componentContainer;
    private RouteInterchangeRepository routeInterchanges;
    private StationRepository stationRepository;
    private TramRouteHelper tramRouteHelper;
    private InterchangeRepository interchangeRepository;
    private RouteRepository routeRepository;
    private LocalDate when;

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
        routeRepository = componentContainer.get(RouteRepository.class);
        routeInterchanges = componentContainer.get(RouteInterchangeRepository.class);
        tramRouteHelper = new TramRouteHelper();
        interchangeRepository = componentContainer.get(InterchangeRepository.class);

        when = TestEnv.testDay();

    }

    @Test
    void shouldGetInterchangesForARoute() {

        Route route = tramRouteHelper.getOneRoute(KnownTramRoute.BuryManchesterAltrincham, routeRepository, when);

        Set<InterchangeStation> interchangesForRoute = routeInterchanges.getFor(route);

        assertFalse(interchangesForRoute.isEmpty());
    }

    @Summer2022
    @Test
    void shouldGetCostToInterchangeForRouteStation() {

       Route route = tramRouteHelper.getOneRoute(KnownTramRoute.AltrinchamPiccadilly, routeRepository, when);

        List<RouteStation> navigationRoadRouteStations = stationRepository.getRouteStationsFor(NavigationRoad.getId()).stream().
                filter(routeStation -> route.equals(routeStation.getRoute())).collect(Collectors.toList());

        assertFalse(navigationRoadRouteStations.isEmpty());

        RouteStation navigationRoad = navigationRoadRouteStations.get(0);

        Duration cost = routeInterchanges.costToInterchange(navigationRoad);

        // cost to trafford bar
        //assertMinutesEquals(14, cost);
        assertMinutesEquals(2, cost);

    }

    @Test
    void shouldGetCostToInterchangeForRouteStationAdjacent() {

        Route route = tramRouteHelper.getOneRoute(KnownTramRoute.AltrinchamPiccadilly, routeRepository, when);

        List<RouteStation> oldTraffordRouteStations = stationRepository.getRouteStationsFor(OldTrafford.getId()).stream().
                filter(routeStation -> route.equals(routeStation.getRoute())).collect(Collectors.toList());

        assertFalse(oldTraffordRouteStations.isEmpty());

        RouteStation oldTrafford = oldTraffordRouteStations.get(0);

        Duration cost = routeInterchanges.costToInterchange(oldTrafford);

        // cost to trafford bar
        assertMinutesEquals(2, cost);

    }

    @Test
    void shouldGetZeroCostToInterchangeForRouteStationThatIsInterchange() {

        Route route = tramRouteHelper.getOneRoute(KnownTramRoute.AltrinchamPiccadilly, routeRepository, when);

        List<RouteStation> cornbrookRouteStations = stationRepository.getRouteStationsFor(Cornbrook.getId()).
                stream().
                filter(routeStation -> route.equals(routeStation.getRoute())).
                collect(Collectors.toList());

        assertFalse(cornbrookRouteStations.isEmpty());

        cornbrookRouteStations.forEach(routeStation -> {
                    Duration cost = routeInterchanges.costToInterchange(routeStation);
                    assertTrue(cost.isZero());
                }
        );
    }

    @Test
    void shouldGetMaxCostIfNoInterchangeBetweenStationAndEndOfTheRoute() {

        Route towardsEndOfEnd = tramRouteHelper.getOneRoute(KnownTramRoute.VictoriaWythenshaweManchesterAirport, routeRepository, when);

        List<RouteStation> peelHallRouteStations = stationRepository.getRouteStationsFor(PeelHall.getId()).stream().
                filter(routeStation -> routeStation.getRoute().equals(towardsEndOfEnd)).collect(Collectors.toList());

        assertFalse(peelHallRouteStations.isEmpty());

        peelHallRouteStations.forEach(routeStation -> {
            Duration cost = routeInterchanges.costToInterchange(routeStation);
            assertTrue(cost.isNegative());
        });

    }

    @Test
    void shouldHaveConsistencyOnZeroCostToInterchangeAndInterchanges() {
        Set<RouteStation> zeroCostToInterchange = stationRepository.getRouteStations().stream().
                filter(routeStation -> routeInterchanges.costToInterchange(routeStation).isZero()).
                collect(Collectors.toSet());

        Set<RouteStation> zeroCostButNotInterchange = zeroCostToInterchange.stream().
                filter(zeroCost -> !interchangeRepository.isInterchange(zeroCost.getStation())).
                collect(Collectors.toSet());

        assertTrue(zeroCostButNotInterchange.isEmpty(), zeroCostButNotInterchange.toString());
    }



}



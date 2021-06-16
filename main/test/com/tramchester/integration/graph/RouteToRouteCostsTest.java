package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdSet;
import com.tramchester.graph.search.RouteToRouteCosts;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.KnownTramRoute;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.testSupport.reference.KnownTramRoute.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RouteToRouteCostsTest {

    private static ComponentContainer componentContainer;

    private RouteToRouteCosts routeCosts;
    private TramRouteHelper routeHelper;

    @BeforeAll
    static void onceBeforeAnyTestRuns() {
        TramchesterConfig config = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        routeCosts = componentContainer.get(RouteToRouteCosts.class);
        RouteRepository routeRepository = componentContainer.get(RouteRepository.class);
        routeHelper = new TramRouteHelper(routeRepository);
    }

    @Test
    void shouldHaveExpectedNumberOfInterconnections() {
        assertEquals(144, routeCosts.size());
    }

    @Test
    void shouldComputeCostsSameRoute() {
        Route routeA = routeHelper.get(AltrinchamPiccadilly);

        assertEquals(0,routeCosts.getFor(routeA, routeA));
    }

    @Test
    void shouldComputeCostsRouteOtherDirection() {
        Route routeA = routeHelper.get(AltrinchamPiccadilly);
        Route routeB = routeHelper.get(KnownTramRoute.PiccadillyAltrincham);

        assertEquals(1,routeCosts.getFor(routeA, routeB));
        assertEquals(1,routeCosts.getFor(routeB, routeA));
    }

    @Test
    void shouldComputeCostsDifferentRoutes() {
        // only ever one hop between routes on tram network
        Route routeA = routeHelper.get(AltrinchamPiccadilly);
        Route routeB = routeHelper.get(VictoriaWythenshaweManchesterAirport);

        assertEquals(1,routeCosts.getFor(routeA, routeB));
        assertEquals(1,routeCosts.getFor(routeB, routeA));
    }

    @Test
    void shouldSortAsExpected() {
        Route routeA = routeHelper.get(CornbrookTheTraffordCentre);
        Route routeB = routeHelper.get(VictoriaWythenshaweManchesterAirport);
        Route routeC = routeHelper.get(BuryPiccadilly);

        List<HasId<Route>> toSort = Arrays.asList(routeC, routeB, routeA);

        IdSet<Route> destinations = new IdSet<>(routeA.getId());
        Stream<HasId<Route>> results = routeCosts.sortByDestinations(toSort.stream(), destinations);

        List<HasId<Route>> list = results.collect(Collectors.toList());
        assertEquals(toSort.size(), list.size());
        assertEquals(routeA.getId(), list.get(0).getId());
        assertEquals(routeB.getId(), list.get(1).getId());
        assertEquals(routeC.getId(), list.get(2).getId());
    }
}

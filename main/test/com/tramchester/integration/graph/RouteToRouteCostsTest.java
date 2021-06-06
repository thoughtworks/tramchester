package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
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
        Route routeA = routeHelper.get(KnownTramRoute.AltrinchamPiccadilly);

        assertEquals(0,routeCosts.getFor(routeA, routeA));
    }

    @Test
    void shouldComputeCostsRouteOtherDirection() {
        Route routeA = routeHelper.get(KnownTramRoute.AltrinchamPiccadilly);
        Route routeB = routeHelper.get(KnownTramRoute.PiccadillyAltrincham);

        assertEquals(1,routeCosts.getFor(routeA, routeB));
        assertEquals(1,routeCosts.getFor(routeB, routeA));
    }

    @Test
    void shouldComputeCostsDifferentRoutes() {
        // only ever one hop between routes on tram network
        Route routeA = routeHelper.get(KnownTramRoute.AltrinchamPiccadilly);
        Route routeB = routeHelper.get(KnownTramRoute.VictoriaWythenshaweManchesterAirport);

        assertEquals(1,routeCosts.getFor(routeA, routeB));
        assertEquals(1,routeCosts.getFor(routeB, routeA));
    }
}

package com.tramchester.integration.dataimport;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.IdSet;
import com.tramchester.domain.Route;
import com.tramchester.domain.reference.KnownRoute;
import com.tramchester.integration.testSupport.IntegrationTramTestConfig;
import com.tramchester.repository.RouteRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnownRouteTest {
    private static ComponentContainer componentContainer;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder().create(new IntegrationTramTestConfig());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @Test
    void shouldHaveCorrespondanceWithLoadedRoutes() {
        RouteRepository routeRepository = componentContainer.get(RouteRepository.class);

        Set<Route> loadedRoutes = routeRepository.getRoutes();
        List<KnownRoute> routes = Arrays.asList(KnownRoute.values());

        assertEquals(routes.size(), loadedRoutes.size());

        IdSet<Route> knownRouteIds = routes.stream().map(KnownRoute::getId).collect(IdSet.idCollector());

        for (Route loaded: loadedRoutes) {
            IdFor<Route> id = loaded.getId();
            assertTrue(knownRouteIds.contains(id), id.toString());
        }

        Set<String> knownRouteNames = routes.stream().map(Enum::name).collect(Collectors.toSet());

        for (Route loaded: loadedRoutes) {
            String name = loaded.getName().replace(" ","").replace("-","");
            assertTrue(knownRouteNames.contains(name), name);
        }
    }
}

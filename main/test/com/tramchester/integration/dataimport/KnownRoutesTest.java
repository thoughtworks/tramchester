package com.tramchester.integration.dataimport;

import com.tramchester.Dependencies;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.IdSet;
import com.tramchester.domain.Route;
import com.tramchester.domain.reference.KnownRoutes;
import com.tramchester.integration.IntegrationTramTestConfig;
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

class KnownRoutesTest {
    private static Dependencies dependencies;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTramTestConfig());
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Test
    void shouldHaveCorrespondanceWithLoadedRoutes() {
        RouteRepository routeRepository = dependencies.get(RouteRepository.class);

        Set<Route> loadedRoutes = routeRepository.getRoutes();
        List<KnownRoutes> routes = Arrays.asList(KnownRoutes.values());

        assertEquals(routes.size(), loadedRoutes.size());

        IdSet<Route> knownRouteIds = routes.stream().map(KnownRoutes::getId).collect(IdSet.idCollector());

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

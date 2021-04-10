package com.tramchester.integration.dataimport;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Route;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnownTramRouteTest {
    private static ComponentContainer componentContainer;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder().create(new IntegrationTramTestConfig(), TestEnv.NoopRegisterMetrics());
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
        List<KnownTramRoute> knownRoutes = Arrays.asList(KnownTramRoute.values());

        assertEquals(knownRoutes.size(), loadedRoutes.size());

//        IdSet<Route> knownRouteIds = knownRoutes.stream().map(KnownTramRoute::getId).collect(IdSet.idCollector());

//        for (Route loaded: loadedRoutes) {
//            IdFor<Route> id = loaded.getId();
//            assertTrue(knownRouteIds.contains(id), id.toString());
//        }

        Set<String> knownRouteNames = knownRoutes.stream().map(Enum::name).collect(Collectors.toSet());

        for (Route loaded: loadedRoutes) {
            String name = loaded.getName().replace(" ","").replace("-","");
            assertTrue(knownRouteNames.contains(name), name);
        }

        for(KnownTramRoute known : knownRoutes) {
            Route found = TestEnv.findTramRoute(routeRepository, known);
            String name = found.getName().replace(" ","").replace("-","");
            assertEquals(name, known.name(), known.name());
        }


    }
}

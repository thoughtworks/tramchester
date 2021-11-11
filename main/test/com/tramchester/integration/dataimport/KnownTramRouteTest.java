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
    void shouldHaveMatchWithLoadedRoutes() {
        RouteRepository routeRepository = componentContainer.get(RouteRepository.class);
        List<KnownTramRoute> knownRoutes = Arrays.asList(KnownTramRoute.values());
        Set<String> knownRouteNames = knownRoutes.stream().map(KnownTramRoute::longName).collect(Collectors.toSet());

        Set<Route> loadedRoutes = routeRepository.getRoutes();

        // NOTE: not checking numbers here as loaded data can contain the 'same' route but with different id

        for (Route loaded: loadedRoutes) {
            final String loadedName = loaded.getName();
            assertTrue(knownRouteNames.contains(loadedName), "missing from known '" + loadedName+"'");

            KnownTramRoute matched = knownRoutes.stream().
                    filter(knownTramRoute -> knownTramRoute.longName().equals(loadedName)).findFirst().orElseThrow();

            assertEquals(loaded.getShortName(), matched.shortName());
            assertEquals(loaded.getTransportMode(), matched.mode());
        }

        Set<String> loadedRouteNames = loadedRoutes.stream().map(Route::getName).collect(Collectors.toSet());

        for (KnownTramRoute knownTramRoute : knownRoutes) {
            assertTrue(loadedRouteNames.contains(knownTramRoute.longName()), "Missing from loaded " + knownTramRoute);
        }

    }
}

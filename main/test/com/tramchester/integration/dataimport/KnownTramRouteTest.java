package com.tramchester.integration.dataimport;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.DataUpdateTest;
import com.tramchester.testSupport.testTags.Summer2022;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DataUpdateTest
class KnownTramRouteTest {
    private static ComponentContainer componentContainer;
    private RouteRepository routeRepository;
    private List<KnownTramRoute> knownRoutes;
    private TramDate when;


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
    void setUp() {
        when = TestEnv.testDay();

        knownRoutes = KnownTramRoute.getFor(when);
        routeRepository = componentContainer.get(RouteRepository.class);
    }

    @Test
    void shouldHaveMatchWithLoadedRoutes() {
        Set<String> knownRouteNames = knownRoutes.stream().map(KnownTramRoute::longName).collect(Collectors.toSet());

        Set<Route> loadedRoutes = routeRepository.getRoutes().stream().filter(route -> route.isAvailableOn(when)).collect(Collectors.toSet());

        // NOTE: not checking numbers here as loaded data can contain the 'same' route but with different id

        for (Route loaded: loadedRoutes) {
            final String loadedName = loaded.getName();
            assertTrue(knownRouteNames.contains(loadedName), "missing from known '" + loaded.getName()+"' " + loaded.getId());

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

    @Test
    void shouldHaveNameAndDirectionCorrect() {

        Set<KnownTramRoute> missing = knownRoutes.stream().
                filter(knownTramRoute -> !knownTramRoute.isReplacement()).
                filter(knownTramRoute -> routeRepository.getRouteById(knownTramRoute.getFakeId()) == null).
                collect(Collectors.toSet());

        assertTrue(missing.isEmpty(), missing.toString());

        Set<Pair<String, IdFor<Route>>> mismatch = knownRoutes.stream().
                filter(knownTramRoute -> !knownTramRoute.isReplacement()).
                map(knownTramRoute -> Pair.of(knownTramRoute, routeRepository.getRouteById(knownTramRoute.getFakeId()))).
                filter(pair -> !pair.getLeft().longName().equals(pair.getRight().getName())).
                map(pair -> Pair.of(pair.getLeft().name(), pair.getRight().getId())).
                collect(Collectors.toSet());

        assertTrue(mismatch.isEmpty(), mismatch.toString());

    }

    @Summer2022
    @Test
    void shouldHaveDateOverlapsForAllKnownRoutes() {

        IdSet<Route> availableRoutes = routeRepository.getRoutes().stream().filter(route -> route.isAvailableOn(when)).collect(IdSet.collector());

        assertNotEquals(0, availableRoutes.size());

        assertEquals(knownRoutes.size() , availableRoutes.size(), availableRoutes.toString());

    }

}

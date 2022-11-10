package com.tramchester.integration.dataimport;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.testSupport.testTags.DataUpdateTest;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

        Set<Route> loadedRoutes = getLoadedRoutes().collect(Collectors.toSet());

        // NOTE: not checking numbers here as loaded data can contain the 'same' route but with different id

        for (Route loaded : loadedRoutes) {
            final String loadedName = loaded.getName();
            assertTrue(knownRouteNames.contains(loadedName), "missing from known '" + loaded.getName() + "' " + loaded.getId());

            KnownTramRoute matched = knownRoutes.stream().
                    filter(knownTramRoute -> knownTramRoute.longName().equals(loadedName)).findFirst().orElseThrow();

            assertEquals(loaded.getShortName(), matched.shortName());
            assertEquals(loaded.getTransportMode(), matched.mode());
        }
    }

    @Test
    void shouldHaveKnownRouteInLoadedData() {

        Set<String> loadedRouteNames = getLoadedRoutes().map(Route::getName).collect(Collectors.toSet());

        for (KnownTramRoute knownTramRoute : knownRoutes) {
            assertTrue(loadedRouteNames.contains(knownTramRoute.longName()), "Missing from loaded " + knownTramRoute);
        }

    }

    @NotNull
    private Stream<Route> getLoadedRoutes() {
        return routeRepository.getRoutes().stream().filter(route -> route.isAvailableOn(when));
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

    @Test
    void shouldHaveDateOverlapsForAllKnownRoutes() {

        IdSet<Route> availableRoutes = routeRepository.getRoutesRunningOn(when).stream().collect(IdSet.collector());;

        assertNotEquals(0, availableRoutes.size());

        assertEquals(knownRoutes.size() , availableRoutes.size(), availableRoutes.toString());

    }

    @Test
    void shouldHaveCorrectIdsForKnownRoutesForTestDate() {

        IdSet<Route> fromRepos = routeRepository.getRoutesRunningOn(when).stream().collect(IdSet.collector());

        IdSet<Route> onDate = KnownTramRoute.getFor(when).stream().map(KnownTramRoute::getFakeId).collect(IdSet.idCollector());

        IdSet<Route> mismatch = IdSet.difference(fromRepos, onDate);

        assertTrue(mismatch.isEmpty(), "mismatch " + mismatch + " between expected " + fromRepos + " and " + onDate);
    }

    @Test
    void shouldHaveCorrectIdsForKnownRoutesForToday() {

        // NOTE: REPLACEMENT services ids can change, so check that if mismatch here

        TramDate date = TramDate.from(TestEnv.LocalNow());

        IdSet<Route> fromRepos = routeRepository.getRoutesRunningOn(date).stream().collect(IdSet.collector());

        IdSet<Route> onDate = KnownTramRoute.getFor(date).stream().map(KnownTramRoute::getFakeId).collect(IdSet.idCollector());

        IdSet<Route> mismatch = IdSet.difference(fromRepos, onDate);

        assertTrue(mismatch.isEmpty(), "mismatch " + mismatch + " between expected " + fromRepos + " and " + onDate);
    }

    @Test
    void shouldHaveCorrectNamesForKnownRoutesForTestDate() {

        Set<String> fromRepos = routeRepository.getRoutesRunningOn(when).stream().map(Route::getName).collect(Collectors.toSet());

        Set<String> onDate = KnownTramRoute.getFor(when).stream().map(KnownTramRoute::longName).collect(Collectors.toSet());

        Set<String> mismatch = SetUtils.difference(fromRepos, onDate);

        assertTrue(mismatch.isEmpty(), "mismatch " + mismatch + " between expected " + fromRepos + " and " + onDate);
    }

    @Test
    void shouldHaveCorrectNamesForKnownRoutesForToday() {

        // NOTE: REPLACEMENT services ids can change, so check that if mismatch here

        TramDate date = TramDate.from(TestEnv.LocalNow());

        Set<String> fromRepos = routeRepository.getRoutesRunningOn(date).stream().map(Route::getName).collect(Collectors.toSet());

        Set<String> onDate = KnownTramRoute.getFor(date).stream().map(KnownTramRoute::longName).collect(Collectors.toSet());

        Set<String> mismatch = SetUtils.difference(fromRepos, onDate);

        assertTrue(mismatch.isEmpty(), "mismatch " + mismatch + " between expected " + fromRepos + " and " + onDate);
    }

    @Test
    void shouldValidateRoutesOverTimePeriod() {
        TramDate base = TramDate.from(TestEnv.LocalNow());

        List<TramDate> failedDates = new ArrayList<>();

        for (int day = 0; day < 14; day++) {
            TramDate testDate = base.plusDays(day);
            IdSet<Route> expected = routeRepository.getRoutesRunningOn(testDate).stream().collect(IdSet.collector());

            List<KnownTramRoute> onDate = KnownTramRoute.getFor(testDate);

            if (expected.size()!=onDate.size()) {
                failedDates.add(testDate);
            }
        }

        assertTrue(failedDates.isEmpty(), failedDates.toString());

    }

}

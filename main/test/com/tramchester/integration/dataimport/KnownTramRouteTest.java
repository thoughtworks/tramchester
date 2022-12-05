package com.tramchester.integration.dataimport;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.integration.testSupport.ConfigParameterResolver;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.testSupport.testTags.DataUpdateTest;
import com.tramchester.testSupport.testTags.DualTest;
import org.apache.commons.collections4.SetUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ConfigParameterResolver.class)
@DualTest
@DataUpdateTest
class KnownTramRouteTest {
    private static ComponentContainer componentContainer;
    private RouteRepository routeRepository;
    private TramDate when;

    @BeforeAll
    static void onceBeforeAnyTestsRun(TramchesterConfig tramchesterConfig) {
        componentContainer = new ComponentsBuilder().create(tramchesterConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void setUp() {
        when = TestEnv.testDay();
        routeRepository = componentContainer.get(RouteRepository.class);
    }

    /// Note: START HERE when diagnosing
    @Test
    void shouldHaveCorrectLongNamesForKnownRoutesForDates() {

        TramDate start = TramDate.from(TestEnv.LocalNow());

        DateRange dateRange = DateRange.of(start, when);

        dateRange.stream().forEach(date -> {
            Set<String> loadedLongNames = getLoadedTramRoutes(date).map(Route::getName).collect(Collectors.toSet());

            Set<String> knownTramOnDates = KnownTramRoute.getFor(date).stream().map(KnownTramRoute::longName).collect(Collectors.toSet());

            Set<String> mismatch = SetUtils.disjunction(loadedLongNames, knownTramOnDates);

            assertTrue(mismatch.isEmpty(), "on " + date + "mismatch " + mismatch + " between LOADED " + loadedLongNames + " AND " + knownTramOnDates);
        });

    }

    @Test
    void shouldHaveShortNameAndDirectionMatching() {
        // Assumes long name match, if this fails get shouldHaveCorrectLongNamesForKnownRoutesForDates working first

        TramDate start = TramDate.from(TestEnv.LocalNow());

        DateRange dateRange = DateRange.of(start, when);

        dateRange.stream().forEach(date -> {
            Set<Route> loadedRoutes = getLoadedTramRoutes(date).collect(Collectors.toSet());
            KnownTramRoute.getFor(date).forEach(knownTramRoute -> {
                List<Route> findLoadedFor = loadedRoutes.stream().
                        filter(loadedRoute -> loadedRoute.getName().equals(knownTramRoute.longName())).
                        collect(Collectors.toList());
                assertEquals(1, findLoadedFor.size(), "Could not find loaded route using long name match for " + knownTramRoute);
                Route loadedRoute = findLoadedFor.get(0);
                assertEquals(loadedRoute.getShortName(), knownTramRoute.shortName(), "On " + date + " short name incorrect for " + knownTramRoute);
                assertTrue(loadedRoute.getId().forDTO().contains(knownTramRoute.direction().getSuffix()),
                        "On " + date + " direction incorrect for " + knownTramRoute + " " + knownTramRoute.direction() +" and ID " + loadedRoute.getId());
            });
        });
    }

    @Test
    void shouldNotHaveUnusedKnownTramRoutes() {
        TramDate start = TramDate.from(TestEnv.LocalNow());

        DateRange dateRange = DateRange.of(start, when);

        // returned for dates, and hence tested
        Set<KnownTramRoute> returnedForDates = dateRange.stream().flatMap(date -> KnownTramRoute.getFor(date).stream()).collect(Collectors.toSet());

        Set<KnownTramRoute> all = EnumSet.allOf(KnownTramRoute.class);

        SetUtils.SetView<KnownTramRoute> diff = SetUtils.disjunction(returnedForDates, all);

        assertTrue(diff.isEmpty(), "Expected empty, are they still needed, got " + diff);

    }

    @NotNull
    private Stream<Route> getLoadedTramRoutes(TramDate date) {
        return routeRepository.getRoutesRunningOn(date).stream().filter(route -> route.getTransportMode() == Tram);
    }

}

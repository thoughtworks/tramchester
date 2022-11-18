package com.tramchester.integration.repository;

import com.google.common.collect.Sets;
import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.Route;
import com.tramchester.domain.RoutePair;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.testSupport.testTags.DataUpdateTest;
import com.tramchester.testSupport.testTags.PiccGardens2022;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.reference.KnownTramRoute.*;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

@DataUpdateTest
public class RouteRepositoryTest {

    private static GuiceContainerDependencies componentContainer;
    private RouteRepository routeRepository;
    private TramRouteHelper routeHelper;
    private StationRepository stationRepository;
    private TramDate when;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationTramTestConfig config = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        routeRepository = componentContainer.get(RouteRepository.class);
        stationRepository = componentContainer.get(StationRepository.class);
        routeHelper = new TramRouteHelper(routeRepository);

        when = TestEnv.testDay();
    }

    @PiccGardens2022
    @Test
    void shouldGetRouteWithHeadsigns() {
        Route result = routeHelper.getOneRoute(AshtonUnderLyneManchesterEccles, when);
        //assertEquals("Ashton Under Lyne - Manchester - Eccles", result.getName());
        assertEquals("Manchester - Eccles", result.getName());
        assertEquals(TestEnv.MetAgency(),result.getAgency());
        assertTrue(result.getId().forDTO().startsWith("METLBLUE:I:"));
        assertTrue(TransportMode.isTram(result));
    }

    @Test
    void extraRouteAtShudehillTowardsEcclesFromVictoria() {
        Route towardsEcclesRoute = routeRepository.getRouteById(StringIdFor.createId("METLBLUE:I:CURRENT"));
        List<Trip> ecclesTripsViaShudehill = towardsEcclesRoute.getTrips().stream().
                filter(trip -> trip.callsAt(Shudehill)).collect(Collectors.toList());

        List<StopCall> fromVictoria = ecclesTripsViaShudehill.stream().
                map(trip -> trip.getStopCalls().getFirstStop()).
                filter(stopCall -> stopCall.getStationId().equals(Victoria.getId())).
                collect(Collectors.toList());

        assertEquals(fromVictoria.size(), ecclesTripsViaShudehill.size(), ecclesTripsViaShudehill.toString());
    }

    @Test
    void extraRouteAtShudehillFromEcclesToVictoria() {
        Route towardsEcclesRoute = routeRepository.getRouteById(StringIdFor.createId("METLBLUE:O:CURRENT"));
        List<Trip> ecclesTripsViaShudehill = towardsEcclesRoute.getTrips().stream().
                filter(trip -> trip.getStopCalls().callsAt(Shudehill)).collect(Collectors.toList());

        List<StopCall> toVictoria = ecclesTripsViaShudehill.stream().
                map(trip -> trip.getStopCalls().getLastStop()).
                filter(stopCall -> stopCall.getStationId().equals(Victoria.getId())).
                collect(Collectors.toList());

        assertEquals(toVictoria.size(), ecclesTripsViaShudehill.size(), ecclesTripsViaShudehill.toString());
    }

    @Test
    void shouldHaveEndOfLinesExpectedPickupAndDropoffRoutes() {
        Route fromAltrincamToBury = routeHelper.getOneRoute(AltrinchamManchesterBury, when);
        Route fromBuryToAltrincham = routeHelper.getOneRoute(BuryManchesterAltrincham, when);

        Station endOfLine = stationRepository.getStationById(Altrincham.getId());

        assertFalse(endOfLine.servesRouteDropOff(fromAltrincamToBury));
        assertTrue(endOfLine.servesRoutePickup(fromAltrincamToBury));

        assertTrue(endOfLine.servesRouteDropOff(fromBuryToAltrincham));
        assertFalse(endOfLine.servesRoutePickup(fromBuryToAltrincham));

        // should serve both routes fully
        Station notEndOfLine = stationRepository.getStationById(NavigationRoad.getId());

        assertTrue(notEndOfLine.servesRouteDropOff(fromAltrincamToBury));
        assertTrue(notEndOfLine.servesRoutePickup(fromAltrincamToBury));
        assertTrue(notEndOfLine.servesRouteDropOff(fromBuryToAltrincham));
        assertTrue(notEndOfLine.servesRoutePickup(fromBuryToAltrincham));
    }

    @Test
    void shouldHaveExpectedNumberOfRoutesRunning() {
        Set<String> running = routeRepository.getRoutesRunningOn(when).stream().map(Route::getName).collect(Collectors.toSet());
        Set<String> knownTramRoutes = getFor(when).stream().map(KnownTramRoute::longName).collect(Collectors.toSet());

        Sets.SetView<String> diffA = Sets.difference(running, knownTramRoutes);
        assertTrue(diffA.isEmpty(), diffA.toString());

        Sets.SetView<String> diffB = Sets.difference(knownTramRoutes, running);
        assertTrue(diffB.isEmpty(), diffB.toString());

        assertEquals(knownTramRoutes.size(), running.size());
    }

    @Test
    void shouldOverlapAsExpected() {

        Set<KnownTramRoute> known = KnownTramRoute.getFor(when);
        Set<RoutePair> noOverlap = new HashSet<>();

        for (KnownTramRoute knownRouteA : known) {
            for (KnownTramRoute knownRouteB : known) {
                Route routeA = routeHelper.getOneRoute(knownRouteA, when);
                Route routeB = routeHelper.getOneRoute(knownRouteB, when);
                if (!routeA.isDateOverlap(routeB)) {
                    noOverlap.add(RoutePair.of(routeA, routeB));
                }
            }
        }

        assertTrue(noOverlap.isEmpty(), noOverlap.toString());

    }

    @Test
    void shouldReproIssueWithUnsymmetricDateOverlap() {

        TramDate date = when.plusWeeks(2);

        Route routeA = routeHelper.getOneRoute(BuryPiccadilly, date); // routeRepository.getRouteById(createId("METLYELL:I:CURRENT"));
        Route routeB = routeHelper.getOneRoute(AltrinchamManchesterBury, date); // routeRepository.getRouteById(createId("METLGREE:I:CURRENT"));

        assertTrue(routeA.isAvailableOn(date));
        assertTrue(routeB.isAvailableOn(date));

        assertTrue(routeA.isDateOverlap(routeB), "no overlap for " + routeA + " and " + routeB);
        assertTrue(routeB.isDateOverlap(routeA), "no overlap for " + routeB + " and " + routeA);
    }

}

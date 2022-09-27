package com.tramchester.integration.repository;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.MutableRoute;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.domain.Route.createId;
import static com.tramchester.testSupport.reference.KnownTramRoute.AshtonUnderLyneManchesterEccles;
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

    @Test
    void shouldGetRouteWithHeadsigns() {
        Set<Route> results = TestEnv.findTramRoute(routeRepository, AshtonUnderLyneManchesterEccles);
        results.forEach(result -> {
            assertEquals("Ashton Under Lyne - Manchester - Eccles", result.getName());
            assertEquals(TestEnv.MetAgency(),result.getAgency());
            assertTrue(result.getId().forDTO().startsWith("METLBLUE:I:"));
            assertTrue(TransportMode.isTram(result));
        });
    }

    @Test
    void extraRouteAtShudehillTowardsEcclesFromVictoria() {
        Route towardsEcclesRoute = routeRepository.getRouteById(StringIdFor.createId("METLBLUE:I:CURRENT"));
        List<Trip> ecclesTripsViaShudehill = towardsEcclesRoute.getTrips().stream().
                filter(trip -> trip.getStopCalls().callsAt(Shudehill)).collect(Collectors.toList());

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
        Route fromAltrincamToPicc = routeRepository.getRouteById(createId("METLPURP:I:CURRENT"));
        Route fromPiccToAltrincham = routeRepository.getRouteById(createId("METLPURP:O:CURRENT"));

        Station endOfLine = stationRepository.getStationById(Altrincham.getId());

        assertFalse(endOfLine.servesRouteDropOff(fromAltrincamToPicc));
        assertTrue(endOfLine.servesRoutePickup(fromAltrincamToPicc));

        assertTrue(endOfLine.servesRouteDropOff(fromPiccToAltrincham));
        assertFalse(endOfLine.servesRoutePickup(fromPiccToAltrincham));

        Station notEndOfLine = stationRepository.getStationById(NavigationRoad.getId());

        assertTrue(notEndOfLine.servesRouteDropOff(fromAltrincamToPicc));
        assertTrue(notEndOfLine.servesRoutePickup(fromAltrincamToPicc));
        assertTrue(notEndOfLine.servesRouteDropOff(fromPiccToAltrincham));
        assertTrue(notEndOfLine.servesRoutePickup(fromPiccToAltrincham));
    }

    @Test
    void shouldHaveExpectedNumberOfRoutesRunning() {
        Set<Route> running = routeRepository.getRoutesRunningOn(when);
        assertEquals(KnownTramRoute.values().length, running.size());
    }

    @Test
    void shouldOverlapAsExpected() {

        KnownTramRoute[] known = KnownTramRoute.values();
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

        Route routeA = routeRepository.getRouteById(createId("METLYELL:I:CURRENT"));
        Route routeB = routeRepository.getRouteById(createId("METLGREE:I:CURRENT"));

        assertTrue(routeA.isAvailableOn(when));
        assertTrue(routeB.isAvailableOn(when));

        assertTrue(routeA.isDateOverlap(routeB), "no overlap for " + routeA + " and " + routeB);
        assertTrue(routeB.isDateOverlap(routeA), "no overlap for " + routeB + " and " + routeA);
    }

}

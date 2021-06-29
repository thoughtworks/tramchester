package com.tramchester.integration.repository;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.RouteCallingStations;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.KnownTramRoute;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.TestEnv.assertIdEquals;
import static com.tramchester.testSupport.reference.KnownTramRoute.*;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

class RouteCallingStationsTest {

    private static ComponentContainer componentContainer;
    private RouteCallingStations callingStations;
    private RouteRepository transportData;
    private TramRouteHelper routeHelper;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder().create(new IntegrationTramTestConfig(), TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        callingStations = componentContainer.get(RouteCallingStations.class);
        transportData = componentContainer.get(RouteRepository.class);
        routeHelper = new TramRouteHelper(componentContainer);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @Test
    void shouldHaveVictoriaToAirportCorrectStopsCityZone() {
        List<Station> stations = getStationsFor(VictoriaWythenshaweManchesterAirport);
        assertIdEquals(Victoria, stations.get(0));
        assertIdEquals(Shudehill, stations.get(1));
        assertIdEquals(MarketStreet, stations.get(2));
        assertIdEquals(StWerburghsRoad, stations.get(9));
    }

    @Test
    void shouldGetCorrectStationsForARouteAltToPicc() {
        List<Station> stations = getStationsFor(AltrinchamPiccadilly);
        assertIdEquals(Altrincham, stations.get(0));
        assertIdEquals(NavigationRoad, stations.get(1));
        assertIdEquals(Cornbrook, stations.get(9));
        assertIdEquals(Piccadilly, stations.get(13));
    }

    @Test
    void shouldGetCorrectStationsForARouteEcclesToAsh() {
        List<Station> stations = getStationsFor(EcclesManchesterAshtonUnderLyne);
        assertIdEquals(Eccles, stations.get(0));
        assertIdEquals(MediaCityUK, stations.get(5));
        assertIdEquals(Deansgate, stations.get(12));
        assertIdEquals(Ashton, stations.get(27));
    }

    @Test
    void shouldGetCorrectStationsForARouteAshToEccles() {
        List<Station> stations = getStationsFor(AshtonUnderLyneManchesterEccles);
        assertIdEquals(Ashton, stations.get(0));
        assertIdEquals(Eccles, stations.get(27));
        assertIdEquals(MediaCityUK, stations.get(22));
        assertIdEquals(Piccadilly, stations.get(12));
    }

    @Test
    void shouldHaveEndsOfLines() {
        IdSet<Station> foundEndOfLines = new IdSet<>();
        transportData.getRoutes().forEach(route -> {
            List<Station> stations = callingStations.getStationsFor(route);
            foundEndOfLines.add(stations.get(0).getId());
        });
        assertEquals(11, foundEndOfLines.size());
    }

    @Test
    void shouldGetCorrectNumberOfStationsForRoutes() {
        assertEquals(14, getStationsFor(AltrinchamPiccadilly).size());
        assertEquals(14, getStationsFor(PiccadillyAltrincham).size());

        // not 27, some journeys skip mediacity UK
        assertEquals(28, getStationsFor(AshtonUnderLyneManchesterEccles).size());
        assertEquals(28, getStationsFor(EcclesManchesterAshtonUnderLyne).size());

        assertEquals(33, getStationsFor(EastDidisburyManchesterShawandCromptonRochdale).size());
        assertEquals(33, getStationsFor(RochdaleShawandCromptonManchesterEastDidisbury).size());

        assertEquals(25, getStationsFor(ManchesterAirportWythenshaweVictoria).size());
        assertEquals(25, getStationsFor(VictoriaWythenshaweManchesterAirport).size());

        assertEquals(8, getStationsFor(TheTraffordCentreCornbrook).size());
        assertEquals(8, getStationsFor(CornbrookTheTraffordCentre).size());

        assertEquals(15, getStationsFor(PiccadillyBury).size());
        assertEquals(15, getStationsFor(BuryPiccadilly).size());

    }

    private List<Station> getStationsFor(KnownTramRoute knownRoute) {
        final Set<Route> routes = routeHelper.get(knownRoute);
        List<List<Station>> stationsForRoute = routes.stream().map(route -> callingStations.getStationsFor(route)).collect(Collectors.toList());

        assertFalse(stationsForRoute.isEmpty(), "found none for " + knownRoute);

        // should all be the same
        List<Station> first = stationsForRoute.get(0);
        final int found = stationsForRoute.size();
        if (found >1) {
            for (int i = 1; i < found; i++) {
                assertEquals(first, stationsForRoute.get(i));
            }
        }

        return first;
    }

}

package com.tramchester.integration.repository;

import com.tramchester.ComponentContainer;
import com.tramchester.Dependencies;
import com.tramchester.domain.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.KnownRoute;
import com.tramchester.integration.testSupport.IntegrationTramTestConfig;
import com.tramchester.repository.RouteCallingStations;
import com.tramchester.repository.TransportData;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.tramchester.domain.reference.KnownRoute.*;
import static com.tramchester.testSupport.reference.RoutesForTesting.createTramRoute;
import static com.tramchester.testSupport.TestEnv.assertIdEquals;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RouteCallingStationsTest {

    private static ComponentContainer componentContainer;
    private static RouteCallingStations repo;
    private static TransportData transportData;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new Dependencies();
        componentContainer.initialise(new IntegrationTramTestConfig());
        repo = componentContainer.get(RouteCallingStations.class);
        transportData = componentContainer.get(TransportData.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @Test
    void shouldHaveVictoriaToAirportCorrectStopsCityZone() {
        List<Station> stations = getStationsFor(VictoriaManchesterAirport);
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
        List<Station> stations = getStationsFor(EcclesManchesterAshtonunderLyne);
        assertIdEquals(Eccles, stations.get(0));
        assertIdEquals(MediaCityUK, stations.get(5));
        assertIdEquals(Deansgate, stations.get(12));
        assertIdEquals(Ashton, stations.get(27));
    }

    @Test
    void shouldGetCorrectStationsForARouteAshToEccles() {
        List<Station> stations = getStationsFor(AshtonunderLyneManchesterEccles);
        assertIdEquals(Ashton, stations.get(0));
        assertIdEquals(Eccles, stations.get(27));
        assertIdEquals(MediaCityUK, stations.get(22));
        assertIdEquals(Piccadilly, stations.get(12));
    }

    @Test
    void shouldHaveEndsOfLines() {
        IdSet<Station> foundEndOfLines = new IdSet<>();
        transportData.getRoutes().forEach(route -> {
            List<Station> stations = repo.getStationsFor(route);
            foundEndOfLines.add(stations.get(0).getId());
        });
        assertEquals(11, foundEndOfLines.size());
    }

    @Test
    void shouldGetCorrectNumberOfStationsForRoutes() {
        assertEquals(14, getStationsFor(AltrinchamPiccadilly).size());
        assertEquals(14, getStationsFor(PiccadillyAltrincham).size());

        // not 27, some journeys skip mediacity UK
        assertEquals(28, getStationsFor(AshtonunderLyneManchesterEccles).size());
        assertEquals(28, getStationsFor(EcclesManchesterAshtonunderLyne).size());

        assertEquals(33, getStationsFor(EDidsburyManchesterRochdale).size());
        assertEquals(33, getStationsFor(RochdaleManchesterEDidsbury).size());

        assertEquals(25, getStationsFor(ManchesterAirportVictoria).size());
        assertEquals(25, getStationsFor(VictoriaManchesterAirport).size());

        assertEquals(8, getStationsFor(intuTraffordCentreCornbrook).size());
        assertEquals(8, getStationsFor(CornbrookintuTraffordCentre).size());

        assertEquals(15, getStationsFor(PiccadillyBury).size());
        assertEquals(15, getStationsFor(BuryPiccadilly).size());

    }

    private List<Station> getStationsFor(KnownRoute knownRoute) {
        return repo.getStationsFor(createTramRoute(knownRoute));
    }

}

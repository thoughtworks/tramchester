package com.tramchester.integration.repository;

import com.tramchester.Dependencies;
import com.tramchester.domain.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.RouteCallingStations;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.RoutesForTesting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.tramchester.testSupport.TramStations.*;

class RouteCallingStationsTest {

    private static Dependencies dependencies;
    private static RouteCallingStations repo;
    private static TransportData transportData;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTramTestConfig());
        repo = dependencies.get(RouteCallingStations.class);
        transportData = dependencies.get(TransportData.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Test
    void shouldHaveVictoriaToAirportCorrectStopsCityZone() {
        List<Station> stations = repo.getStationsFor(RoutesForTesting.VIC_TO_AIR);
        Assertions.assertEquals(Victoria.getId(), stations.get(0).getId());
        Assertions.assertEquals(Shudehill.getId(), stations.get(1).getId());
        Assertions.assertEquals(MarketStreet.getId(), stations.get(2).getId());
        Assertions.assertEquals(StWerburghsRoad.getId(), stations.get(9).getId());
    }

    @Test
    void shouldGetCorrectStationsForARouteAltToPicc() {
        List<Station> stations = repo.getStationsFor(RoutesForTesting.ALTY_TO_PICC);
        Assertions.assertEquals(Altrincham.getId(), stations.get(0).getId());
        Assertions.assertEquals(NavigationRoad.getId(), stations.get(1).getId());
        Assertions.assertEquals(Cornbrook.getId(), stations.get(9).getId());
        Assertions.assertEquals(Piccadilly.getId(), stations.get(13).getId());
    }

    @Test
    void shouldGetCorrectStationsForARouteEcclesToAsh() {
        List<Station> stations = repo.getStationsFor(RoutesForTesting.ECCLES_TO_ASH);
        Assertions.assertEquals(Eccles.getId(), stations.get(0).getId());
        Assertions.assertEquals(MediaCityUK.getId(), stations.get(5).getId());
        Assertions.assertEquals(Deansgate.getId(), stations.get(12).getId());
        Assertions.assertEquals(Ashton.getId(), stations.get(27).getId());
    }

    @Test
    void shouldGetCorrectStationsForARouteAshToEccles() {
        List<Station> stations = repo.getStationsFor(RoutesForTesting.ASH_TO_ECCLES);
        Assertions.assertEquals(Ashton.getId(), stations.get(0).getId());
        Assertions.assertEquals(Eccles.getId(), stations.get(27).getId());
        Assertions.assertEquals(MediaCityUK.getId(), stations.get(22).getId());
        Assertions.assertEquals(Piccadilly.getId(), stations.get(12).getId());
    }

    @Test
    void shouldHaveEndsOfLines() {
        IdSet<Station> foundEndOfLines = new IdSet<>();
        transportData.getRoutes().forEach(route -> {
            List<Station> stations = repo.getStationsFor(route);
            foundEndOfLines.add(stations.get(0).getId());
        });
        Assertions.assertEquals(11, foundEndOfLines.size());
    }

    @Test
    void shouldGetCorrectNumberOfStationsForRoutes() {
        Assertions.assertEquals(14, repo.getStationsFor(RoutesForTesting.ALTY_TO_PICC).size());
        Assertions.assertEquals(14, repo.getStationsFor(RoutesForTesting.PICC_TO_ALTY).size());

        // not 27, some journeys skip mediacity UK
        Assertions.assertEquals(28, repo.getStationsFor(RoutesForTesting.ASH_TO_ECCLES).size());
        Assertions.assertEquals(28, repo.getStationsFor(RoutesForTesting.ECCLES_TO_ASH).size());

        Assertions.assertEquals(33, repo.getStationsFor(RoutesForTesting.DIDS_TO_ROCH).size());
        Assertions.assertEquals(33, repo.getStationsFor(RoutesForTesting.ROCH_TO_DIDS).size());

        Assertions.assertEquals(25, repo.getStationsFor(RoutesForTesting.AIR_TO_VIC).size());
        Assertions.assertEquals(25, repo.getStationsFor(RoutesForTesting.VIC_TO_AIR).size());

        Assertions.assertEquals(8, repo.getStationsFor(RoutesForTesting.INTU_TO_CORN).size());
        Assertions.assertEquals(8, repo.getStationsFor(RoutesForTesting.CORN_TO_INTU).size());

        Assertions.assertEquals(15, repo.getStationsFor(RoutesForTesting.BURY_TO_PICC).size());
        Assertions.assertEquals(15, repo.getStationsFor(RoutesForTesting.PICC_TO_BURY).size());

    }

}

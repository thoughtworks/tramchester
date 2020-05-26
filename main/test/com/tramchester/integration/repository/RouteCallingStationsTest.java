package com.tramchester.integration.repository;

import com.tramchester.Dependencies;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.RouteCallingStations;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.RoutesForTesting;
import com.tramchester.testSupport.Stations;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class RouteCallingStationsTest {

    private static Dependencies dependencies;
    private static RouteCallingStations repo;
    private static TransportData transportData;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws IOException {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTramTestConfig());
        repo = dependencies.get(RouteCallingStations.class);
        transportData = dependencies.get(TransportData.class);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Test
    public void shouldGetCorrectStationsForARouteAltToPicc() {
        List<Station> stations = repo.getStationsFor(RoutesForTesting.ALTY_TO_PICC);
        assertEquals(Stations.Altrincham.getId(), stations.get(0).getId());
        assertEquals(Stations.NavigationRoad.getId(), stations.get(1).getId());
        assertEquals(Stations.Cornbrook.getId(), stations.get(9).getId());
        assertEquals(Stations.Piccadilly.getId(), stations.get(13).getId());
    }

    @Test
    public void shouldGetCorrectStationsForARouteEcclesToAsh() {
        List<Station> stations = repo.getStationsFor(RoutesForTesting.ECCLES_TO_ASH);
        assertEquals(Stations.Eccles.getId(), stations.get(0).getId());
        assertEquals(Stations.MediaCityUK.getId(), stations.get(5).getId());
        assertEquals(Stations.Deansgate.getId(), stations.get(12).getId());
        assertEquals(Stations.Ashton.getId(), stations.get(27).getId());
    }

    @Test
    public void shouldHaveEndsOfLines() {
        Set<String> foundEndOfLines = new HashSet<>();
        transportData.getRoutes().forEach(route -> {
            List<Station> stations = repo.getStationsFor(route);
            foundEndOfLines.add(stations.get(0).getId());
        });
        assertEquals(11, foundEndOfLines.size());
    }

    @Test
    public void shouldGetCorrectNumberOfStationsForRoutes() {
        assertEquals(14, repo.getStationsFor(RoutesForTesting.ALTY_TO_PICC).size());
        assertEquals(14, repo.getStationsFor(RoutesForTesting.PICC_TO_ALTY).size());

        // not 27, some journeys skip mediacity UK
        assertEquals(28, repo.getStationsFor(RoutesForTesting.ASH_TO_ECCLES).size());
        assertEquals(28, repo.getStationsFor(RoutesForTesting.ECCLES_TO_ASH).size());

        assertEquals(33, repo.getStationsFor(RoutesForTesting.DIDS_TO_ROCH).size());
        assertEquals(33, repo.getStationsFor(RoutesForTesting.ROCH_TO_DIDS).size());

        assertEquals(25, repo.getStationsFor(RoutesForTesting.AIR_TO_VIC).size());
        assertEquals(25, repo.getStationsFor(RoutesForTesting.VIC_TO_AIR).size());

        assertEquals(8, repo.getStationsFor(RoutesForTesting.INTU_TO_CORN).size());
        assertEquals(8, repo.getStationsFor(RoutesForTesting.CORN_TO_INTU).size());

        assertEquals(15, repo.getStationsFor(RoutesForTesting.BURY_TO_PICC).size());
        assertEquals(15, repo.getStationsFor(RoutesForTesting.PICC_TO_BURY).size());

    }

}

package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.graph.TramRouteReachable;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.integration.Stations;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class TramRouteReachableTest {

    private static Dependencies dependencies;
    private static IntegrationTramTestConfig testConfig;
    private TramRouteReachable reachable;
    private String manAirportToVictoria = "MET:   6:O:";
    private String victoriaToManAirport = "MET:   6:I:";

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws IOException {
        dependencies = new Dependencies();
        testConfig = new IntegrationTramTestConfig();
        dependencies.initialise(testConfig);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Before
    public void beforeEachTestRuns() {
        reachable = dependencies.get(TramRouteReachable.class);
    }

    @Test
    public void shouldHaveCorrectReachabilitySPIKE() {

        assertTrue(reachable.getRouteReachable(Stations.ManAirport.getId(), Stations.StWerburghsRoad.getId(), manAirportToVictoria));
        assertFalse(reachable.getRouteReachable(Stations.ManAirport.getId(), Stations.StWerburghsRoad.getId(), victoriaToManAirport));

        assertTrue(reachable.getRouteReachable(Stations.StWerburghsRoad.getId(), Stations.ManAirport.getId(), victoriaToManAirport));
        assertFalse(reachable.getRouteReachable(Stations.StWerburghsRoad.getId(), Stations.ManAirport.getId(), manAirportToVictoria));

        assertFalse(reachable.getRouteReachable(Stations.Altrincham.getId(), Stations.ManAirport.getId(), manAirportToVictoria));

    }

    @Test
    public void shouldHaveCorrectReachabilityOrInterchanges() {
        String altyToPicc = "MET:   2:I:";
        assertTrue(reachable.getRouteReachableWithInterchange(Stations.NavigationRoad.getId(), Stations.ManAirport.getId(), altyToPicc));
        String piccToAlty = "MET:   2:O:";
        assertFalse(reachable.getRouteReachableWithInterchange(Stations.NavigationRoad.getId(), Stations.ManAirport.getId(), piccToAlty));

        assertTrue(reachable.getRouteReachableWithInterchange(Stations.ManAirport.getId(), Stations.StWerburghsRoad.getId(), manAirportToVictoria));
        assertFalse(reachable.getRouteReachableWithInterchange(Stations.ManAirport.getId(), Stations.StWerburghsRoad.getId(), victoriaToManAirport));
    }
}

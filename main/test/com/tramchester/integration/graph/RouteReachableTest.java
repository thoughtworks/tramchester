package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Station;
import com.tramchester.graph.RouteReachable;
import com.tramchester.testSupport.BusTest;
import com.tramchester.testSupport.RouteCodesForTesting;
import com.tramchester.testSupport.Stations;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestConfig;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;

import static junit.framework.TestCase.*;
import static org.junit.Assume.assumeTrue;

@RunWith(Parameterized.class)
public class RouteReachableTest {
    private RouteReachable reachable;
    private String manAirportToVictoria = "MET:   6:O:";
    private String victoriaToManAirport = "MET:   6:I:";
    private StationRepository stationRepository;
    private GraphDatabaseService database;
    private Transaction tx;
    private TramchesterConfig config;

    @Parameterized.Parameters
    public static Iterable<? extends Object> data() throws IOException {
        return TestConfig.getDependencies();
    }

    @Parameterized.Parameter
    public Dependencies dependencies;

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        TestConfig.closeDependencies();
    }

    @Before
    public void beforeEachTestRuns() {
        config = dependencies.getConfig();
        stationRepository = dependencies.get(StationRepository.class);
        reachable = dependencies.get(RouteReachable.class);
        database = dependencies.get(GraphDatabaseService.class);
        tx = database.beginTx();
    }

    @After
    public void afterEachTestRuns() {
        tx.close();
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

    @Test
    public void shouldHaveCorrectReachabilityMonsalToRochs() {
        assertTrue(reachable.getRouteReachableWithInterchange(Stations.RochdaleRail.getId(), Stations.Monsall.getId(), RouteCodesForTesting.ROCH_TO_DIDS));
        assertTrue(reachable.getRouteReachableWithInterchange(Stations.Monsall.getId(), Stations.RochdaleRail.getId(), RouteCodesForTesting.DIDS_TO_ROCH));
    }

    @Test
    public void shouldHaveAdjacentReachableCorrect() {

        assertEquals(2,reachable.getRoutesFromStartToNeighbour(getReal(Stations.NavigationRoad), Stations.Altrincham.getId()).size());
        assertEquals(2, reachable.getRoutesFromStartToNeighbour(getReal(Stations.Altrincham), Stations.NavigationRoad.getId()).size());

        // 5 not the 7 on the map, only 6 routes modelled in timetable data, 1 of which does not go between these 2
        assertEquals(5, reachable.getRoutesFromStartToNeighbour(getReal(Stations.Deansgate), Stations.StPetersSquare.getId()).size());

        assertEquals(2, reachable.getRoutesFromStartToNeighbour(getReal(Stations.StPetersSquare), Stations.PiccadillyGardens.getId()).size());
        assertEquals(2, reachable.getRoutesFromStartToNeighbour(getReal(Stations.StPetersSquare), Stations.MarketStreet.getId()).size());

        assertEquals(0, reachable.getRoutesFromStartToNeighbour(getReal(Stations.Altrincham), Stations.Cornbrook.getId()).size());
    }

    @Test
    public void shouldComputeSimpleCostBetweenStations() {
        assertEquals(60, reachable.getApproxCostBetween(Stations.Bury.getId(), Stations.Altrincham.getId()));
        assertEquals(59, reachable.getApproxCostBetween(Stations.Altrincham.getId(), Stations.Bury.getId()));

        assertEquals(5, reachable.getApproxCostBetween(Stations.NavigationRoad.getId(), Stations.Altrincham.getId()));
        assertEquals(6, reachable.getApproxCostBetween(Stations.Altrincham.getId(), Stations.NavigationRoad.getId()));

        assertEquals(61, reachable.getApproxCostBetween(Stations.MediaCityUK.getId(), Stations.ManAirport.getId()));
        assertEquals(61, reachable.getApproxCostBetween(Stations.ManAirport.getId(), Stations.MediaCityUK.getId()));
    }

    @Ignore("WIP")
    @Category({BusTest.class})
    @Test
    public void shouldTestSomethingForBuses() {
        assumeTrue(config.getBus());
        fail("todo for buses only");
    }

    private Station getReal(Station testStation) {
        return stationRepository.getStation(testStation.getId());
    }
}

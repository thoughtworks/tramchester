package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.RouteReachable;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.Stations;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;

import static com.tramchester.testSupport.RoutesForTesting.*;
import static org.junit.jupiter.api.Assertions.*;

class TramRouteReachableTest {
    private static Dependencies dependencies;

    private RouteReachable reachable;
    private StationRepository stationRepository;
    private GraphDatabase database;
//    private Transaction txn;

    @BeforeAll
    static void onceBeforeAnyTestRuns() throws IOException {
        dependencies = new Dependencies();
        TramchesterConfig config = new IntegrationTramTestConfig();
        dependencies.initialise(config);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {

        stationRepository = dependencies.get(StationRepository.class);
        reachable = dependencies.get(RouteReachable.class);
        database = dependencies.get(GraphDatabase.class);
        //txn = database.beginTx();
    }

    @AfterEach
    void afterEachTestRuns() {
//        txn.close();
    }

    @Test
    void shouldHaveCorrectReachabilityOrInterchanges() {
        assertTrue(reachable.getRouteReachableWithInterchange(ALTY_TO_PICC, Stations.NavigationRoad.getId(), Stations.ManAirport.getId()
        ));
        assertFalse(reachable.getRouteReachableWithInterchange(PICC_TO_ALTY, Stations.NavigationRoad.getId(), Stations.ManAirport.getId()
        ));

        assertTrue(reachable.getRouteReachableWithInterchange(AIR_TO_VIC, Stations.ManAirport.getId(), Stations.StWerburghsRoad.getId()
        ));
        assertFalse(reachable.getRouteReachableWithInterchange(VIC_TO_AIR, Stations.ManAirport.getId(), Stations.StWerburghsRoad.getId()
        ));
    }

    @Test
    void shouldHaveCorrectReachabilityMonsalToRochs() {
        assertTrue(reachable.getRouteReachableWithInterchange(ROCH_TO_DIDS, Stations.RochdaleRail.getId(), Stations.Monsall.getId()
        ));
        assertTrue(reachable.getRouteReachableWithInterchange(DIDS_TO_ROCH, Stations.Monsall.getId(), Stations.RochdaleRail.getId()
        ));
    }

    @Test
    void shouldHaveAdjacentRoutesCorrectly() {

        // TODO Lockdown 2->1 for next two tests, only one route to alty now
        assertEquals(1,reachable.getRoutesFromStartToNeighbour(getReal(Stations.NavigationRoad), Stations.Altrincham.getId()).size());
        assertEquals(1, reachable.getRoutesFromStartToNeighbour(getReal(Stations.Altrincham), Stations.NavigationRoad.getId()).size());

        // 5 not the 7 on the map, only 6 routes modelled in timetable data, 1 of which does not go between these 2
        // TODO Lockdown 5->4
        assertEquals(4, reachable.getRoutesFromStartToNeighbour(getReal(Stations.Deansgate), Stations.StPetersSquare.getId()).size());

        assertEquals(2, reachable.getRoutesFromStartToNeighbour(getReal(Stations.StPetersSquare), Stations.PiccadillyGardens.getId()).size());

        // TODO Lockdown 2->1
        assertEquals(1, reachable.getRoutesFromStartToNeighbour(getReal(Stations.StPetersSquare), Stations.MarketStreet.getId()).size());

        assertEquals(0, reachable.getRoutesFromStartToNeighbour(getReal(Stations.Altrincham), Stations.Cornbrook.getId()).size());
    }

    @Test
    void shouldComputeSimpleCostBetweenStations() {

        try(Transaction txn = database.beginTx()) {
            assertEquals(5, reachable.getApproxCostBetween(txn, Stations.NavigationRoad, Stations.Altrincham));
            assertEquals(6, reachable.getApproxCostBetween(txn, Stations.Altrincham, Stations.NavigationRoad));

            assertEquals(62, reachable.getApproxCostBetween(txn, Stations.Bury, Stations.Altrincham));
            assertEquals(62, reachable.getApproxCostBetween(txn, Stations.Altrincham, Stations.Bury));

            assertEquals(61, reachable.getApproxCostBetween(txn, Stations.MediaCityUK, Stations.ManAirport));
            assertEquals(61, reachable.getApproxCostBetween(txn, Stations.ManAirport, Stations.MediaCityUK));
        }
    }


    private Station getReal(Station testStation) {
        return stationRepository.getStation(testStation.getId());
    }
}

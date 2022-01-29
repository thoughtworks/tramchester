package com.tramchester.integration.graph.stateMachine;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.search.LowestCostsForDestRoutes;
import com.tramchester.graph.search.RouteToRouteCosts;
import com.tramchester.graph.search.stateMachine.TraversalOps;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TripRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import static com.tramchester.testSupport.reference.KnownLocations.nearPiccGardens;
import static com.tramchester.testSupport.reference.TramStations.ManAirport;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TraversalOpsTest {
    private static ComponentContainer componentContainer;

    private NodeContentsRepository nodeOperations;
    private TripRepository tripRepository;
    private SortsPositions sortsPositions;
    private StationRepository stationRepository;
    private Transaction txn;
    private RouteToRouteCosts routeToRouteCosts;

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
    void onceBeforEachTestRuns() {
        nodeOperations = componentContainer.get(NodeContentsRepository.class);
        tripRepository = componentContainer.get(TripRepository.class);
        sortsPositions = componentContainer.get(SortsPositions.class);
        stationRepository = componentContainer.get(StationRepository.class);
        routeToRouteCosts = componentContainer.get(RouteToRouteCosts.class);
        GraphDatabase database = componentContainer.get(GraphDatabase.class);
        txn = database.beginTx();
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldHaveCorrectOrderingCompare() {
        TramServiceDate queryDate = new TramServiceDate(TestEnv.testDay());

        LocationSet destinationStations = new LocationSet();
        final Station manchesterAirport = stationRepository.getStationById(ManAirport.getId());
        destinationStations.add(manchesterAirport);
        LatLong destinationLatLon = nearPiccGardens.latLong();

        LowestCostsForDestRoutes lowestCostForRoutes = routeToRouteCosts.getLowestCostCalcutatorFor(destinationStations);

        TraversalOps traversalOpsForDest = new TraversalOps(nodeOperations, tripRepository,
                sortsPositions, destinationStations, destinationLatLon, lowestCostForRoutes, queryDate);

        Station altrincham = stationRepository.getStationById(TramStations.Altrincham.getId());

        HasId<Route> pickupAtAlty = altrincham.getPickupRoutes().iterator().next();
        Route vicToAirport = manchesterAirport.getDropoffRoutes().iterator().next();

        assertEquals(0, traversalOpsForDest.onDestRouteFirst(vicToAirport, vicToAirport));
        assertEquals(-1, traversalOpsForDest.onDestRouteFirst(vicToAirport, pickupAtAlty), "wrong for " + vicToAirport.getId() + " " + pickupAtAlty.getId());
        assertEquals(+1, traversalOpsForDest.onDestRouteFirst(pickupAtAlty, vicToAirport));
        assertEquals(0, traversalOpsForDest.onDestRouteFirst(pickupAtAlty, pickupAtAlty));

    }

}

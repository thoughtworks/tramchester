package com.tramchester.integration.graph.stateMachine;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.search.RouteToRouteCosts;
import com.tramchester.graph.search.stateMachine.TraversalOps;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TripRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.util.HashSet;
import java.util.Set;

import static com.tramchester.testSupport.reference.TramStations.ManAirport;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TraversalOpsTest {
    private static ComponentContainer componentContainer;

    private NodeContentsRepository nodeOperations;
    private TripRepository tripRepository;
    private SortsPositions sortsPositions;
    private StationRepository stationRepository;
    private RouteToRouteCosts numberHops;
    private Transaction txn;


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
        numberHops = componentContainer.get(RouteToRouteCosts.class);
        GraphDatabase database = componentContainer.get(GraphDatabase.class);
        txn = database.beginTx();
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldHaveCorrectOrderingCompare() {

        Set<Station> destinationStations = new HashSet<>();
        final Station manchesterAirport = stationRepository.getStationById(ManAirport.getId());
        destinationStations.add(manchesterAirport);
        LatLong destinationLatLon = TestEnv.nearPiccGardens;
        TraversalOps traversalOps = new TraversalOps(nodeOperations, tripRepository,
                sortsPositions, destinationStations, destinationLatLon);

        Station altrincham = stationRepository.getStationById(TramStations.Altrincham.getId());
        HasId<Route> otherRoute = altrincham.getRoutes().iterator().next();
        HasId<Route> vicToAirport = manchesterAirport.getRoutes().iterator().next();

        assertEquals(0,traversalOps.onDestRouteFirst(vicToAirport, vicToAirport));
        assertEquals(-1,traversalOps.onDestRouteFirst(vicToAirport, otherRoute));
        assertEquals(+1,traversalOps.onDestRouteFirst(otherRoute, vicToAirport));
        assertEquals(0,traversalOps.onDestRouteFirst(otherRoute, otherRoute));

        Station sameRouteStation = stationRepository.getStationById(TramStations.StWerburghsRoad.getId());
        HasId<Route> sameRoute = sameRouteStation.getRoutes().iterator().next();

        assertEquals(0,traversalOps.onDestRouteFirst(sameRoute, vicToAirport));
        assertEquals(0,traversalOps.onDestRouteFirst(vicToAirport, sameRoute));
    }

}

package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.RouteCostCalculator;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.testSupport.reference.TramStations;
import org.checkerframework.checker.units.qual.K;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.util.Set;

import static com.tramchester.testSupport.reference.KnownTramRoute.*;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class RouteCostCalculatorTest {

    private static ComponentContainer componentContainer;

    private RouteCostCalculator routeCostCalc;
    private Transaction txn;
    private StationRepository stationRepository;
    private RouteRepository routeRepository;

    @BeforeAll
    static void onceBeforeAnyTestRuns() {
        TramchesterConfig config = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        stationRepository = componentContainer.get(StationRepository.class);
        routeRepository = componentContainer.get(RouteRepository.class);
        routeCostCalc = componentContainer.get(RouteCostCalculator.class);
        GraphDatabase database = componentContainer.get(GraphDatabase.class);
        txn = database.beginTx();
    }

    @AfterEach
    void afterEachTestHasRun() {
        txn.close();
    }

    @Test
    void shouldComputeSimpleCostBetweenStationsAltyNavRoad() {
        assertEquals(5, getApproxCostBetween(txn, TramStations.NavigationRoad, Altrincham));
        assertEquals(6, getApproxCostBetween(txn, Altrincham, TramStations.NavigationRoad));

    }

    @Test
    void shouldComputeSimpleCostBetweenStationsAltyBury() {
        assertEquals(64, getApproxCostBetween(txn, TramStations.Bury, Altrincham));
        assertEquals(65, getApproxCostBetween(txn, Altrincham, TramStations.Bury));
    }

    @Test
    void shouldComputeSimpleCostBetweenStationsMediaCityAirport() {
        assertEquals(61, getApproxCostBetween(txn, TramStations.MediaCityUK, TramStations.ManAirport));
        assertEquals(61, getApproxCostBetween(txn, TramStations.ManAirport, TramStations.MediaCityUK));
    }

    @Test
    void shouldComputeNumberOfRouteHops() {
        Route route = TestEnv.findTramRoute(routeRepository, AltrinchamPiccadilly);

        RouteStation altyTowardsPicc = stationRepository.getRouteStation(of(Altrincham), route);
        RouteStation stPetersTowardsPicc = stationRepository.getRouteStation(of(StPetersSquare), route);

        assertEquals(12, routeCostCalc.getNumberHops(txn, altyTowardsPicc, stPetersTowardsPicc));
    }

    @Test
    void shouldComputeNumberOfRouteHopsWithChange() {
        Route routeA = TestEnv.findTramRoute(routeRepository, AltrinchamPiccadilly);
        Route routeB = TestEnv.findTramRoute(routeRepository, VictoriaWythenshaweManchesterAirport);

        RouteStation altyTowardsPicc = stationRepository.getRouteStation(of(Altrincham), routeA);
        RouteStation manchesterAirport = stationRepository.getRouteStation(of(ManAirport), routeB);

        assertEquals(27, routeCostCalc.getNumberHops(txn, altyTowardsPicc, manchesterAirport));
    }

    @Test
    void shouldFindNoRoutesIfWrongDirection() {
        Route towardsAlty = TestEnv.findTramRoute(routeRepository, PiccadillyAltrincham);

        RouteStation alty = stationRepository.getRouteStation(of(Altrincham), towardsAlty);
        RouteStation stPeters = stationRepository.getRouteStation(of(StPetersSquare), towardsAlty);

        assertEquals(-1, routeCostCalc.getNumberHops(txn, alty, stPeters));
    }

    private int getApproxCostBetween(Transaction txn, TramStations start, TramStations dest) {
        return routeCostCalc.getApproxCostBetween(txn, of(start), of(dest));
    }

}

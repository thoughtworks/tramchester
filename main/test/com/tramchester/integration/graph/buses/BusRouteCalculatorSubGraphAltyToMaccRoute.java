package com.tramchester.integration.graph.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.DiagramCreator;
import com.tramchester.domain.Journey;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.RouteReachable;
import com.tramchester.graph.graphbuild.ActiveGraphFilter;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.graph.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.IntegrationBusTestConfig;
import com.tramchester.repository.RouteCallingStations;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusStations;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class BusRouteCalculatorSubGraphAltyToMaccRoute {

    private static final IdFor<Route> ROUTE_ID = StringIdFor.createId("DAGC088C:O:");
    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static Config config;

    private RouteCalculatorTestFacade calculator;
    private Transaction txn;
    private TramServiceDate when;
    private List<Station> routeStations;
    private TransportData transportData;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {
        ActiveGraphFilter graphFilter = new ActiveGraphFilter();
        graphFilter.addRoute(ROUTE_ID);

        config = new Config("altyMacRoute.db");
        TestEnv.deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder<>().setGraphFilter(graphFilter).create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        database = componentContainer.get(GraphDatabase.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() throws IOException {
        componentContainer.close();
        TestEnv.deleteDBIfPresent(config);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        transportData = componentContainer.get(TransportData.class);
        RouteCallingStations routeCallingStations = componentContainer.get(RouteCallingStations.class);
        txn = database.beginTx();
        calculator = new RouteCalculatorTestFacade(componentContainer.get(RouteCalculator.class), transportData, txn);

        when = new TramServiceDate(TestEnv.testDay());
        Route route = transportData.getRouteById(ROUTE_ID);

        routeStations = routeCallingStations.getStationsFor(route);
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldBeFilteringByCorrectRoute() {
        Route route = transportData.getRouteById(ROUTE_ID);
        assertNotNull(route);

        Station start = transportData.getStationById(BusStations.AltrinchamInterchange.getId());
        Station end = transportData.getStationById(BusStations.MacclefieldBusStationBay1.getId());

        RouteStation routeStation = transportData.getRouteStation(start, route);

        RouteReachable routeReachable = componentContainer.get(RouteReachable.class);

        IdSet<Station> result = routeReachable.getRouteReachableWithInterchange(routeStation, IdSet.singleton(end.getId()));
        assertFalse(result.isEmpty());
        assertTrue(result.contains(end.getId()));

        assertTrue(start.getRoutes().contains(route));
        assertTrue(end.getRoutes().contains(route));
    }

    @Test
    void shouldHaveJourneyOneEndToTheOther() {

        TramTime time = TramTime.of(10, 40);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, 0, 120);
        Set<Journey> results = calculator.calculateRouteAsSet(BusStations.AltrinchamInterchange, BusStations.MacclefieldBusStationBay1,
                journeyRequest);

        assertFalse(results.isEmpty());
    }

    @Test
    void shouldHaveSimpleRouteWithStationsAlongTheWay() {

        List<IdFor<Station>> ids = routeStations.stream().map(Station::getId).collect(Collectors.toList());
        int knutsfordIndex = ids.indexOf(StringIdFor.createId("0600MA6022")); // services beyond here are infrequent
        Station firstStation = routeStations.get(0);

        TramTime time = TramTime.of(9, 20);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, 0, 120);
        //journeyRequest.setDiag(true);

        for (int i = 1; i <= knutsfordIndex; i++) {
            Station secondStation = routeStations.get(i);
            Set<Journey> result = calculator.calculateRouteAsSet(firstStation, secondStation, journeyRequest);
            assertFalse(result.isEmpty());
        }
    }

    @Test
    void produceDiagramOfGraphSubset() throws IOException {
        //DiagramCreator creator = new DiagramCreator(database);
        DiagramCreator creator = componentContainer.get(DiagramCreator.class);
        creator.create(format("%s_trams.dot", "altyToMacBuses"), routeStations, Integer.MAX_VALUE);
    }

    private static class Config extends IntegrationBusTestConfig {
        public Config(String dbName) {
            super(dbName);
        }

        @Override
        public boolean getCreateNeighbours() {
            return false;
        }
    }

}

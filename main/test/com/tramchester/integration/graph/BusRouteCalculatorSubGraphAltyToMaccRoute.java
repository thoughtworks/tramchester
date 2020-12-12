package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.DiagramCreator;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.Journey;
import com.tramchester.domain.Route;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.graphbuild.ActiveGraphFilter;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.testSupport.IntegrationBusTestConfig;
import com.tramchester.repository.RouteCallingStations;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.RouteCalculatorTestFacade;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class BusRouteCalculatorSubGraphAltyToMaccRoute {

    private static final IdFor<Route> ROUTE_ID = IdFor.createId("DGC:  88:O:");
    private static ComponentContainer componentContainer;
    private static GraphDatabase database;

    private RouteCalculatorTestFacade calculator;
    private Transaction txn;
    private TramServiceDate when;
    private List<Station> routeStations;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        ActiveGraphFilter graphFilter = new ActiveGraphFilter();
        graphFilter.addRoute(ROUTE_ID);

        TramchesterConfig config = new Config("altyMacRoute");
        componentContainer = new ComponentsBuilder().setGraphFilter(graphFilter).create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        database = componentContainer.get(GraphDatabase.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        TransportData transportData = componentContainer.get(TransportData.class);
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
        int knutsfordIndex = ids.indexOf(IdFor.createId("0600MA6022")); // services beyond here are infrequent
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
        DiagramCreator creator = new DiagramCreator(database, 4);
        creator.create(format("%s_trams.dot", "altyToMacBuses"), routeStations);
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

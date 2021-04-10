package com.tramchester.integration.graph.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.DiagramCreator;
import com.tramchester.domain.Journey;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.CompositeStation;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.RouteReachable;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.graph.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.*;
import com.tramchester.testSupport.ActiveGraphFilter;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.TestEnv.NoopRegisterMetrics;
import static com.tramchester.testSupport.TestEnv.deleteDBIfPresent;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class BusRouteCalculatorSubGraphAltyToMaccRoute {

    private static final IdFor<Route> ROUTE_ID_FOR_FILTER_ONLY = StringIdFor.createId("DAGC088C:O:2021-01-02");
    private static ComponentContainer componentContainer;
    private static Config config;
    private RouteReachable routeReachable;

    private RouteCalculatorTestFacade calculator;
    private Transaction txn;
    private TramServiceDate when;
    private List<Station> routeStations;
    private StationRepository stationRepository;
    private CompositeStationRepository compositeStationRepository;
    private Route route;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {
        ActiveGraphFilter graphFilter = new ActiveGraphFilter();
        graphFilter.addRoute(ROUTE_ID_FOR_FILTER_ONLY);

        config = new Config("altyMacRoute.db");
        deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder<>().setGraphFilter(graphFilter).create(config, NoopRegisterMetrics());
        componentContainer.initialise();

    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() throws IOException {
        componentContainer.close();
        deleteDBIfPresent(config);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        GraphDatabase database = componentContainer.get(GraphDatabase.class);
        routeReachable = componentContainer.get(RouteReachable.class);
        RouteRepository routeRepository = componentContainer.get(TransportData.class);
        stationRepository = componentContainer.get(StationRepository.class);
        compositeStationRepository = componentContainer.get(CompositeStationRepository.class);

        RouteCallingStations routeCallingStations = componentContainer.get(RouteCallingStations.class);

        txn = database.beginTx();
        calculator = new RouteCalculatorTestFacade(componentContainer.get(RouteCalculator.class), stationRepository, txn);

        when = new TramServiceDate(TestEnv.testDay());

        route = routeRepository.findFirstRouteByName(StringIdFor.createId("DAGC"), "Altrincham - Wilmslow - Knutsford - Macclesfield");
        routeStations = routeCallingStations.getStationsFor(route);

    }

    @AfterEach
    void afterEachTestRuns() {
        if (txn!=null) {
            txn.close();
        }
    }

    @Test
    void shouldHaveTheCorrectRouteIdForTheFilter() {
        assertEquals(route.getId(), ROUTE_ID_FOR_FILTER_ONLY);
    }

    @Test
    void shouldBeFilteringByCorrectRoute() {

        CompositeStation start = compositeStationRepository.findByName("Altrincham Interchange");
        assertTrue(start.getRoutes().contains(route));

        CompositeStation end = compositeStationRepository.findByName("Bus Station, Knutsford");
        assertTrue(end.getRoutes().contains(route));

        Set<Station> fromAltyServesRoutes = start.getContained().stream().filter(station -> station.servesRoute(route)).collect(Collectors.toSet());
        assertFalse(fromAltyServesRoutes.isEmpty());

        IdSet<Station> endIds = end.getContained().stream()
                .filter(station -> station.servesRoute(route))
                .map(Station::getId).
                collect(IdSet.idCollector());

        assertFalse(endIds.isEmpty());

        fromAltyServesRoutes.forEach(station -> {
            RouteStation routeStation = stationRepository.getRouteStation(station, route);
            assertNotNull(routeStation, routeStation.toString());

            IdSet<Station> result = getRouteReachableWithInterchange(routeStation, endIds);
            assertFalse(result.isEmpty());
        });

    }

    private IdSet<Station> getRouteReachableWithInterchange(RouteStation start, IdSet<Station> destinations) {

        if (routeReachable.isInterchangeReachable(start)) {
            return destinations;
        }

        return routeReachable.getReachableStations(start);
    }

    @Test
    void shouldHaveJourneyOneEndToTheOther() {
        CompositeStation start = compositeStationRepository.findByName("Altrincham Interchange");
        CompositeStation end = compositeStationRepository.findByName("Bus Station, Knutsford");

        TramTime time = TramTime.of(10, 40);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, 3, 120);
        journeyRequest.setDiag(true);
        Set<Journey> results = calculator.calculateRouteAsSet(start, end, journeyRequest);

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
        DiagramCreator creator = componentContainer.get(DiagramCreator.class);
        creator.create(format("%s.dot", "altyToMacBuses"), routeStations, Integer.MAX_VALUE);
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

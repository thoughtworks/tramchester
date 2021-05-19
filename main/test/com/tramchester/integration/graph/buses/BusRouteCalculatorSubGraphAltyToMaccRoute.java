package com.tramchester.integration.graph.buses;

import com.google.common.collect.Streams;
import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.DiagramCreator;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Agency;
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
import com.tramchester.graph.filters.ConfigurableGraphFilter;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.graph.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.*;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.TestEnv.NoopRegisterMetrics;
import static com.tramchester.testSupport.TestEnv.deleteDBIfPresent;
import static org.junit.jupiter.api.Assertions.*;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class BusRouteCalculatorSubGraphAltyToMaccRoute {

    private static ComponentContainer componentContainer;
    private static TramchesterConfig config;
    private static Set<Route> altyToKnutsford;
    private static Set<Route> knutsfordToAlty;

    private RouteReachable routeReachable;
    private RouteCallingStations routeCallingStations;
    private RouteCalculatorTestFacade calculator;
    private CompositeStationRepository compositeStationRepository;
    private StationRepository stationRepository;

    private Transaction txn;
    private TramServiceDate when;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {

        config = new IntegrationBusTestConfig("altyMacRoute.db");
        deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder().
                configureGraphFilter(BusRouteCalculatorSubGraphAltyToMaccRoute::configureFilter).
                create(config, NoopRegisterMetrics());
        componentContainer.initialise();

        RouteRepository routeRepository = componentContainer.get(TransportData.class);
        IdFor<Agency> agencyId = StringIdFor.createId("DAGC");
        altyToKnutsford = routeRepository.findRoutesByName(agencyId,
                "Altrincham - Wilmslow - Knutsford - Macclesfield");
        knutsfordToAlty = routeRepository.findRoutesByName(agencyId,
                "Macclesfield - Knutsford - Wilmslow - Altrincham");
    }

    private static void configureFilter(ConfigurableGraphFilter graphFilter) {
        altyToKnutsford.forEach(route -> graphFilter.addRoute(route.getId()));
        knutsfordToAlty.forEach(route -> graphFilter.addRoute(route.getId()));
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() throws IOException {
        componentContainer.close();
        deleteDBIfPresent(config);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        GraphDatabase database = componentContainer.get(GraphDatabase.class);

        stationRepository = componentContainer.get(StationRepository.class);

        routeReachable = componentContainer.get(RouteReachable.class);
        compositeStationRepository = componentContainer.get(CompositeStationRepository.class);
        routeCallingStations = componentContainer.get(RouteCallingStations.class);

        txn = database.beginTx();
        calculator = new RouteCalculatorTestFacade(componentContainer.get(RouteCalculator.class), stationRepository, txn);

        when = new TramServiceDate(TestEnv.testDay());
    }

    @AfterEach
    void afterEachTestRuns() {
        if (txn!=null) {
            txn.close();
        }
    }

    @Test
    void shouldBeFilteringByCorrectRoute() {

        altyToKnutsford.forEach(route -> {
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
        });

    }

    private IdSet<Station> getRouteReachableWithInterchange(RouteStation start, IdSet<Station> destinations) {

        if (routeReachable.isInterchangeReachableOnRoute(start)) {
            return destinations;
        }

        return routeReachable.getReachableStationsOnRoute(start);
    }

    @Test
    void shouldHaveJourneyAltyToKnutsford() {
        CompositeStation start = compositeStationRepository.findByName("Altrincham Interchange");
        CompositeStation end = compositeStationRepository.findByName("Bus Station, Knutsford");

        TramTime time = TramTime.of(10, 40);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, 1, 120);
        Set<Journey> results = calculator.calculateRouteAsSet(start, end, journeyRequest);

        assertFalse(results.isEmpty());
    }

    @Test
    void shouldHaveJourneyKnutsfordToAlty() {
        // NOTE: can cause (ignorable) errors on destination station node ID search as some of the these stations are
        // not on the specified filtered routes, so not present in the DB
        CompositeStation start = compositeStationRepository.findByName("Bus Station, Knutsford");
        CompositeStation end = compositeStationRepository.findByName("Altrincham Interchange");

        TramTime time = TramTime.of(11, 20);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, 3, 120);

        Set<Journey> results = calculator.calculateRouteAsSet(start, end, journeyRequest);

        assertFalse(results.isEmpty());
    }

    @Test
    void shouldHaveSimpleRouteWithStationsAlongTheWay() {

        altyToKnutsford.forEach(route -> {
            List<Station> stationsAlongRoute = routeCallingStations.getStationsFor(route);

            List<IdFor<Station>> ids = stationsAlongRoute.stream().map(Station::getId).collect(Collectors.toList());
            int knutsfordIndex = ids.indexOf(StringIdFor.createId("0600MA6022")); // services beyond here are infrequent
            Station firstStation = stationsAlongRoute.get(0);

            TramTime time = TramTime.of(9, 20);
            JourneyRequest journeyRequest = new JourneyRequest(when, time, false, 1, 120);

            for (int i = 1; i <= knutsfordIndex; i++) {
                Station secondStation = stationsAlongRoute.get(i);
                Set<Journey> result = calculator.calculateRouteAsSet(firstStation, secondStation, journeyRequest);
                assertFalse(result.isEmpty());
            }
        });

    }

    @Test
    void produceDiagramOfGraphSubset() throws IOException {
        DiagramCreator creator = componentContainer.get(DiagramCreator.class);
        Set<Station> stations = Streams.concat(altyToKnutsford.stream(), knutsfordToAlty.stream()).
                flatMap(route -> routeCallingStations.getStationsFor(route).stream()).
                collect(Collectors.toSet());
        creator.create(Path.of("AltrichamKnutsfordBuses.dot"), stations, 1, true);
    }


}

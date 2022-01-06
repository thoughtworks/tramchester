package com.tramchester.integration.graph.buses;

import com.google.common.collect.Streams;
import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.DiagramCreator;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.filters.ConfigurableGraphFilter;
import com.tramchester.graph.filters.GraphFilter;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.*;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.TestEnv.NoopRegisterMetrics;
import static com.tramchester.testSupport.TestEnv.deleteDBIfPresent;
import static org.junit.jupiter.api.Assertions.*;

@BusTest
class BusRouteCalculatorSubGraphAltyToMaccRoute {

    private static ComponentContainer componentContainer;
    private static TramchesterConfig config;
    private static Set<Route> altyToKnutsford;
    private static Set<Route> knutsfordToAlty;

    private RouteCalculatorTestFacade calculator;
    private StationRepository stationRepository;

    private Transaction txn;
    private TramServiceDate when;
    private GraphFilter graphFilter;
    private CompositeStationRepository compositeStationRepository;

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
        compositeStationRepository =componentContainer.get(CompositeStationRepository.class);

        txn = database.beginTx();
        calculator = new RouteCalculatorTestFacade(componentContainer.get(RouteCalculator.class), stationRepository, txn);

        graphFilter = componentContainer.get(GraphFilter.class);

        when = new TramServiceDate(TestEnv.testDay());
    }

    @AfterEach
    void afterEachTestRuns() {
        if (txn!=null) {
            txn.close();
        }
    }

    @Test
    void shouldFindRoutesForTest() {
        assertNotNull(altyToKnutsford);
        assertNotNull(knutsfordToAlty);
    }

    @Test
    void shouldHaveJourneyAltyToKnutsford() {
        Station start = findStation("Altrincham Interchange");
        Station end = compositeStationRepository.findByName("Bus Station");

        TramTime time = TramTime.of(10, 40);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, 1,
                120, 2);
        Set<Journey> results = calculator.calculateRouteAsSet(start, end, journeyRequest);

        assertFalse(results.isEmpty());
    }

    @Test
    void shouldHaveJourneyKnutsfordToAlty() {
        // NOTE: can cause (ignorable) errors on destination station node ID search as some of the these stations are
        // not on the specified filtered routes, so not present in the DB

        Station start = compositeStationRepository.findByName("Bus Station");
        assertNotNull(start, compositeStationRepository.getAllComposites().toString());

        Station end = findStation("Altrincham Interchange");

        TramTime time = TramTime.of(11, 20);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false,
                3, 120, 2);

        Set<Journey> results = calculator.calculateRouteAsSet(start, end, journeyRequest);

        assertFalse(results.isEmpty());
    }

    @Test
    void shouldHaveSimpleRouteWithStationsAlongTheWay() {

        // TODO WIP

        altyToKnutsford.forEach(route -> {
            //List<Station> stationsAlongRoute = routeCallingStations.getStationsFor(route);
            route.getTrips().forEach(trip -> {
                StopCalls stopCalls = trip.getStopCalls();

                List<IdFor<Station>> ids = stopCalls.stream().map(stopCall -> stopCall.getStation().getId()).collect(Collectors.toList());

                int knutsfordIndex = ids.indexOf(StringIdFor.createId("0600MA6022")); // services beyond here are infrequent
                Station firstStation = stopCalls.getFirstStop().getStation();

                TramTime time = TramTime.of(9, 20);
                JourneyRequest journeyRequest = new JourneyRequest(when, time, false,
                        1, 120, 1);

                for (int i = 1; i <= knutsfordIndex; i++) {
                    Station secondStation = stopCalls.getStopBySequenceNumber(i).getStation();
                    Set<Journey> result = calculator.calculateRouteAsSet(firstStation, secondStation, journeyRequest);
                    assertFalse(result.isEmpty());
                }
            });


        });

    }

    @Test
    void produceDiagramOfGraphSubset() throws IOException {
        DiagramCreator creator = componentContainer.get(DiagramCreator.class);
        // all station for both sets of routes
        Set<Station> stations = Streams.concat(altyToKnutsford.stream(), knutsfordToAlty.stream()).
                flatMap(route -> route.getTrips().stream()).
                flatMap(trip -> trip.getStopCalls().getStationSequence().stream()).
                collect(Collectors.toSet());
        creator.create(Path.of("AltrichamKnutsfordBuses.dot"), stations, 1, true);
    }


    private Station findStation(String stationName) {
        List<Station> found = stationRepository.getStationsForMode(TransportMode.Bus).stream().
                filter(station -> graphFilter.shouldInclude(station)).
                filter(station -> station.getName().equals(stationName)).collect(Collectors.toList());
        assertEquals(1, found.size());
        return found.get(0);
    }


}

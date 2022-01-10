package com.tramchester.integration.graph.buses;

import com.google.common.collect.Streams;
import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.DiagramCreator;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Agency;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.places.CompositeStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.geo.StationLocations;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.filters.ConfigurableGraphFilter;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@BusTest
class BusRouteCalculatorSubGraphAltyToMaccRoute {

    private static ComponentContainer componentContainer;
    private static TramchesterConfig config;
    private static Set<Route> altyToKnutsford;
    private static Set<Route> knutsfordToAlty;

    private RouteCalculatorTestFacade calculator;

    private Transaction txn;
    private TramServiceDate when;
    private CompositeStationRepository compositeStationRepository;
    private CompositeStation altrinchamInterchange;
    private List<Station> knutfordStations;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {

        config = new IntegrationBusTestConfig("altyMacRoute.db");
        deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder().
                configureGraphFilter(BusRouteCalculatorSubGraphAltyToMaccRoute::configureFilter).
                create(config, NoopRegisterMetrics());
        componentContainer.initialise();

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
        RouteRepository routeRepository = componentContainer.get(TransportData.class);
        IdFor<Agency> agencyId = StringIdFor.createId("DAGC");
        altyToKnutsford = routeRepository.findRoutesByName(agencyId,
                "Altrincham - Wilmslow - Knutsford - Macclesfield");
        knutsfordToAlty = routeRepository.findRoutesByName(agencyId,
                "Macclesfield - Knutsford - Wilmslow - Altrincham");

        GraphDatabase database = componentContainer.get(GraphDatabase.class);

        StationRepository stationRepository = componentContainer.get(StationRepository.class);
        compositeStationRepository = componentContainer.get(CompositeStationRepository.class);

        txn = database.beginTx();

        calculator = new RouteCalculatorTestFacade(componentContainer.get(RouteCalculator.class), stationRepository, txn);

        when = new TramServiceDate(TestEnv.testDay());

        altrinchamInterchange = compositeStationRepository.findByName("Altrincham Interchange");

        LatLong nearKnutsfordBusStation = new LatLong(53.30262,-2.3775267);
        StationLocations stationLocations = componentContainer.get(StationLocations.class);

        // because knutford bus station is just called "Bus Station"
        final MarginInMeters rangeInMeters = MarginInMeters.of(1000);
        knutfordStations = stationLocations.nearestStationsSorted(nearKnutsfordBusStation, 10, rangeInMeters).
                stream().
                filter(station -> station.getName().contains("Bus Station")).
                collect(Collectors.toList());
    }

    @AfterEach
    void afterEachTestRuns() {
        if (txn!=null) {
            txn.close();
        }
    }

    @Test
    void shouldHaveKnutfordBusStation() {
        assertFalse(knutfordStations.isEmpty());
    }

    @Test
    void shouldFindRoutesForTest() {
        assertNotNull(altyToKnutsford);
        assertNotNull(knutsfordToAlty);
    }

    @Test
    void shouldHaveJourneyAltyToKnutsford() {
        Station end = knutfordStations.get(0);

        TramTime time = TramTime.of(10, 40);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, 1,
                120, 2);
        Set<Journey> results = calculator.calculateRouteAsSet(altrinchamInterchange, end, journeyRequest);

        assertFalse(results.isEmpty());
    }

    @Test
    void shouldHaveJourneyKnutsfordToAlty() {
        // NOTE: can cause (ignorable) errors on destination station node ID search as some of the these stations are
        // not on the specified filtered routes, so not present in the DB

        //Station start = compositeStationRepository.findByName("Bus Station");
        Station start = knutfordStations.get(0);
        assertNotNull(start, compositeStationRepository.getAllComposites().toString());

        TramTime time = TramTime.of(11, 20);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false,
                3, 120, 2);

        Set<Journey> results = calculator.calculateRouteAsSet(start, altrinchamInterchange, journeyRequest);

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

}

package com.tramchester.integration.graph.rail;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.graph.search.routes.RouteToRouteCosts;
import com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.TrainTest;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.util.*;

import static com.tramchester.domain.reference.TransportMode.Train;
import static com.tramchester.integration.testSupport.rail.RailStationIds.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@TrainTest
public class RouteToRouteCostsRailTest {
    public static final Set<TransportMode> TRAIN = Collections.singleton(Train);
    private static ComponentContainer componentContainer;
    private static TramDate date;

    private RouteToRouteCosts routeToRouteCosts;
    private StationRepository stationRepository;
    private Transaction txn;
    private Station manPicc;
    private Station stockport;
    private Station londonEuston;
    private TimeRange timeRange;

    @BeforeAll
    static void onceBeforeAnyTestRuns() {
        TramchesterConfig config = new IntegrationRailTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        // clear cache
        //TestEnv.clearDataCache(componentContainer);

        date = TestEnv.testDay();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        //TestEnv.clearDataCache(componentContainer);
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        GraphDatabase database = componentContainer.get(GraphDatabase.class);

        txn = database.beginTx();
        routeToRouteCosts = componentContainer.get(RouteToRouteCosts.class);
        stationRepository = componentContainer.get(StationRepository.class);

        // full rebuild of graph, including version node so we avoid rebuild every test run
        componentContainer.get(StagedTransportGraphBuilder.class);

        manPicc = stationRepository.getStationById(ManchesterPiccadilly.getId());
        stockport = stationRepository.getStationById(Stockport.getId());
        londonEuston = stationRepository.getStationById(LondonEuston.getId());

        timeRange = TimeRange.of(TramTime.of(8,15), TramTime.of(22,35));

    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldGetNumberOfRouteHopsBetweenStockportAndManPicc() {
        assertEquals(0, routeToRouteCosts.getNumberOfChanges(stockport, manPicc, TRAIN, date, timeRange).getMin());
    }

    @Test
    void shouldHaveExpectedNumberHopsChangesManToStockport() {
        assertEquals(0, routeToRouteCosts.getNumberOfChanges(manPicc, stockport, TRAIN, date, timeRange).getMin());
    }

    @Test
    void shouldGetNumberOfRouteHopsBetweenManPiccAndLondonEustom() {
        assertEquals(0, routeToRouteCosts.getNumberOfChanges(manPicc, londonEuston, TRAIN, date, timeRange).getMin());
    }

    @Disabled("Performance testing")
    @Test
    void shouldGetNumberOfRouteHopsBetweenManPiccAndLondonEustomPerformanceTesting() {
        for (int i = 0; i < 20; i++) {
            routeToRouteCosts.getNumberOfChanges(manPicc, londonEuston, TRAIN, date, timeRange);
        }
    }

    @Test
    void shouldGetNumberOfRouteHopsBetweenAltrinchamAndLondonEuston() {
        Station altrincham = stationRepository.getStationById(Altrincham.getId());
        assertEquals(1, routeToRouteCosts.getNumberOfChanges(altrincham, londonEuston, TRAIN, date, timeRange).getMin());
    }

    @Test
    void shouldGetNumberOfRouteHopsBetweenAltrinchamNavigationRoadAndStockport() {
        Station altrincham = stationRepository.getStationById(Altrincham.getId());
        Station navigationRaod = stationRepository.getStationById(NavigationRaod.getId());
        Station stockport = stationRepository.getStationById(Stockport.getId());

        assertEquals(0, routeToRouteCosts.getNumberOfChanges(altrincham, navigationRaod, TRAIN, date, timeRange).getMin());
        assertEquals(0, routeToRouteCosts.getNumberOfChanges(navigationRaod, stockport, TRAIN, date, timeRange).getMin());
        assertEquals(0, routeToRouteCosts.getNumberOfChanges(altrincham, stockport, TRAIN, date, timeRange).getMin());

    }

    @Test
    void shouldGetNumberOfChangesKnutsfordToDover() {
        Station knutsford = stationRepository.getStationById(Knutsford.getId());
        Station other = stationRepository.getStationById(Dover.getId());
        assertEquals(3, routeToRouteCosts.getNumberOfChanges(knutsford, other, TRAIN, date, timeRange).getMin());
    }

    @Disabled("spike only")
    @Test
    void shouldSpikeEquivalentRoutesWhereSetOfInterchangesAreSame() {
        Map<IdSet<Station>, Set<Route>> results = new HashMap<>(); // unique set of interchanges -> routes with those interchanges

        RouteRepository routeRepository = componentContainer.get(RouteRepository.class);

        InterchangeRepository interchangeRepository = componentContainer.get(InterchangeRepository.class);

        Map<Route, IdSet<Station>> interchangesForRoute = new HashMap<>();

        interchangeRepository.getAllInterchanges().forEach(interchangeStation -> {
            Set<Route> dropoffs = interchangeStation.getDropoffRoutes();

            dropoffs.forEach(dropoff -> {
                if (!interchangesForRoute.containsKey(dropoff)) {
                    interchangesForRoute.put(dropoff, new IdSet<>());
                }
                interchangesForRoute.get(dropoff).add(interchangeStation.getStationId());
            });
        });

        interchangesForRoute.forEach((route, interchanges) -> {
            if (!results.containsKey(interchanges)) {
                results.put(interchanges, new HashSet<>());
            }
            results.get(interchanges).add(route);
        });

        assertFalse(results.isEmpty());
        // always fails, here to show size reduction
        // seem to get only 16% reduction in number of routes when comparing by sets of interchanges for uk rail
        assertEquals(routeRepository.numberOfRoutes(), results.size());
    }
}

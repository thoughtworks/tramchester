package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.DiagramCreator;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.filters.ConfigurableGraphFilter;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.KnownTramRoute;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.tramchester.domain.reference.TransportMode.TramsOnly;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Disabled("Just used to track down specific issue")
class RouteCalculatorSubGraphEcclesAshtonLine {
    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static SubgraphConfig config;
    private static TramRouteHelper tramRouteHelper;

    private RouteCalculatorTestFacade calculator;
    private final TramDate when = TestEnv.testDay();

    private Transaction txn;
    private Duration maxJourneyDuration;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {
        config = new SubgraphConfig();
//        TestEnv.deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder().
                configureGraphFilter(RouteCalculatorSubGraphEcclesAshtonLine::configureFilter).
                create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
        database = componentContainer.get(GraphDatabase.class);

        RouteRepository routeRepository = componentContainer.get(RouteRepository.class);
        tramRouteHelper = new TramRouteHelper(routeRepository);

    }

    private static void configureFilter(ConfigurableGraphFilter graphFilter, TransportData transportData) {
        Set<Route> routes = tramRouteHelper.get(KnownTramRoute.EcclesManchesterAshtonUnderLyne);
        routes.forEach(route -> graphFilter.addRoute(route.getId()));
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() throws IOException {
        componentContainer.close();
//        TestEnv.deleteDBIfPresent(config);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        StationRepository stationRepository = componentContainer.get(StationRepository.class);
        txn = database.beginTx();
        calculator = new RouteCalculatorTestFacade(componentContainer.get(RouteCalculator.class), stationRepository, txn);

        maxJourneyDuration = Duration.ofMinutes(config.getMaxJourneyDuration());
    }

    @AfterEach
    void onceAfterEveryTest() {
        txn.close();
    }

    @Test
    void ShouldReproIssueWithMediaCityToVelopark() {
        JourneyRequest request = new JourneyRequest(when, TramTime.of(8, 5), false,
                1, maxJourneyDuration, 2, TramsOnly);
        request.setDiag(true);

        assertFalse(calculator.calculateRouteAsSet(MediaCityUK, VeloPark, request).isEmpty());
    }

    @Test
    void produceDiagramOfGraphSubset() throws IOException {
        DiagramCreator creator = componentContainer.get(DiagramCreator.class);
        List<Station> starts = Arrays.asList(VeloPark.fake(), Etihad.fake());
        creator.create(Path.of("subgraph_eccles_ashton_velo.dot"),starts, 2, true);
    }

    @Test
    void produceDiagramOfGraphSubsetEtihad() throws IOException {
        DiagramCreator creator = componentContainer.get(DiagramCreator.class);
        List<Station> starts = Collections.singletonList(Etihad.fake());
        creator.create(Path.of("subgraph_eccles_ashton_etihad.dot"),starts, 1, false);
    }

    @Test
    void produceDiagramOfGraphSubsetMediaCity() throws IOException {
        DiagramCreator creator = componentContainer.get(DiagramCreator.class);
        List<Station> starts = Arrays.asList(MediaCityUK.fake(), HarbourCity.fake(),
                Broadway.fake());
        creator.create(Path.of("subgraph_eccles_ashton_mediaCity.dot"),starts, 2, true);
    }

    private static class SubgraphConfig extends IntegrationTramTestConfig {
        public SubgraphConfig() {
            super("subgraph_eccles_ashton_tramchester.db", Collections.emptyList());
        }
    }

}

package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.DiagramCreator;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.Route;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.filters.ConfigurableGraphFilter;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.resources.LocationJourneyPlanner;
import com.tramchester.testSupport.LocationJourneyPlannerTestFacade;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.testSupport.reference.TramStations;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RouteCalculatorSubGraphEcclesAshtonLine {
    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static SubgraphConfig config;

    private RouteCalculatorTestFacade calculator;
    private final LocalDate when = TestEnv.testDay();

    private Transaction txn;
    private TramTime tramTime;
    private int maxJourneyDuration;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {
        config = new SubgraphConfig();
        TestEnv.deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder().
                configureGraphFilter(RouteCalculatorSubGraphEcclesAshtonLine::configureFilter).
                create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        database = componentContainer.get(GraphDatabase.class);
    }

    private static void configureFilter(ConfigurableGraphFilter graphFilter) {
        TramRouteHelper tramRouteHelper = new TramRouteHelper(componentContainer);
        Set<Route> routes = tramRouteHelper.get(KnownTramRoute.EcclesManchesterAshtonUnderLyne);
        routes.forEach(route -> graphFilter.addRoute(route.getId()));
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() throws IOException {
        componentContainer.close();
        TestEnv.deleteDBIfPresent(config);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        StationRepository stationRepository = componentContainer.get(StationRepository.class);
        txn = database.beginTx();
        calculator = new RouteCalculatorTestFacade(componentContainer.get(RouteCalculator.class), stationRepository, txn);

        tramTime = TramTime.of(8, 0);
        maxJourneyDuration = config.getMaxJourneyDuration();
    }

    @AfterEach
    void onceAfterEveryTest() {
        txn.close();
    }

    // TODO Investigate so can remove workaround in RouteCalculatorSupport
    @Test
    void ShouldReproIssueWithSomeMediaCityJourneys() {
        JourneyRequest request = new JourneyRequest(when, TramTime.of(8, 5), false,
                2, maxJourneyDuration, 2);

        assertFalse(calculator.calculateRouteAsSet(MediaCityUK, Etihad, request).isEmpty());
//        assertFalse(calculator.calculateRouteAsSet(MediaCityUK, VeloPark, request).isEmpty());
//        assertFalse(calculator.calculateRouteAsSet(MediaCityUK, Ashton, request).isEmpty());
    }

    @Test
    void produceDiagramOfGraphSubset() throws IOException {
        DiagramCreator creator = componentContainer.get(DiagramCreator.class);
        creator.create(Path.of("subgraph_eccles_ashton.dot"), TramStations.of(Cornbrook), 100, false);
    }

    private static class SubgraphConfig extends IntegrationTramTestConfig {
        public SubgraphConfig() {
            super("subgraph_eccles_ashton_tramchester.db");
        }
    }

}

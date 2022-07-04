package com.tramchester.integration.graph.diversions;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.DiagramCreator;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.StationClosure;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphQuery;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.filters.ConfigurableGraphFilter;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.StationClosureForTest;
import com.tramchester.integration.testSupport.tram.IntegrationTramClosedStationsTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.StationsWithDiversionRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.tramchester.graph.graphbuild.GraphLabel.PLATFORM;
import static com.tramchester.graph.graphbuild.GraphLabel.ROUTE_STATION;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

class SubgraphClosedStationsDiversionsTest {
    // Note this needs to be > time for whole test fixture, see note below in @After
    private static final int TXN_TIMEOUT = 5*60;

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static IntegrationTramClosedStationsTestConfig config;

    private static final List<TramStations> centralStations = Arrays.asList(
            Cornbrook,
            Deansgate,
            StPetersSquare,
            ExchangeSquare,
            Victoria,
            MarketStreet,
            Shudehill,
            PiccadillyGardens,
            Piccadilly,
            Monsall);
    private RouteCalculatorTestFacade calculator;
    private StationRepository stationRepository;
    private final static TramServiceDate when = new TramServiceDate(TestEnv.testDay());
    private Transaction txn;

    private final static List<StationClosure> closedStations = Collections.singletonList(
            new StationClosureForTest(TramStations.StPetersSquare, when.getDate(), when.getDate().plusWeeks(1)));
    private Duration maxJourneyDuration;
    private int maxChanges;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new IntegrationTramClosedStationsTestConfig("closed_stpeters_subgraph_tram.db",
                closedStations, true);
        componentContainer = new ComponentsBuilder().
                configureGraphFilter(SubgraphClosedStationsDiversionsTest::configureFilter).
                create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
        database = componentContainer.get(GraphDatabase.class);
    }

    private static void configureFilter(ConfigurableGraphFilter graphFilter, TransportData transportData) {
        centralStations.forEach(station -> graphFilter.addStation(station.getId()));
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() throws IOException {
        componentContainer.close();
        TestEnv.deleteDBIfPresent(config);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        txn = database.beginTx(TXN_TIMEOUT, TimeUnit.SECONDS);
        stationRepository = componentContainer.get(StationRepository.class);
        calculator = new RouteCalculatorTestFacade(componentContainer.get(RouteCalculator.class), stationRepository, txn);
        maxJourneyDuration = Duration.ofMinutes(30);
        maxChanges = 2;
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    private Set<TransportMode> getRequestedModes() {
        return Collections.emptySet();
    }

    @Test
    void shouldHaveTheDiversionsInTheRepository() {
        StationsWithDiversionRepository repository = componentContainer.get(StationsWithDiversionRepository.class);
        assertTrue(repository.hasDiversions(Deansgate.from(stationRepository)));
        assertTrue(repository.hasDiversions(ExchangeSquare.from(stationRepository)));
        assertTrue(repository.hasDiversions(PiccadillyGardens.from(stationRepository)));
        assertTrue(repository.hasDiversions(MarketStreet.from(stationRepository)));

        assertFalse(repository.hasDiversions(Shudehill.from(stationRepository)));
    }

    @Test
    void shouldFindRouteAroundCloseBackOnToTramCornbrookToVictoria() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                maxChanges, maxJourneyDuration, 1, getRequestedModes());

        Set<Journey> results = calculator.calculateRouteAsSet(Cornbrook, Victoria, journeyRequest);

        assertFalse(results.isEmpty(), "no journeys");

        validateStages(results);
    }

    @Test
    void shouldFindRouteAroundCloseBackOnToTramVictoriaToCornbrook() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                maxChanges, maxJourneyDuration, 1, getRequestedModes());
        Set<Journey> results = calculator.calculateRouteAsSet(Victoria, Cornbrook, journeyRequest);

        assertFalse(results.isEmpty(), "no journeys");

        validateStages(results);
    }

    @Test
    void shouldFindRouteAroundCloseBackOnToTramPiccToCornbrook() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                maxChanges, maxJourneyDuration, 1, getRequestedModes());
        Set<Journey> results = calculator.calculateRouteAsSet(Piccadilly, Cornbrook, journeyRequest);

        assertFalse(results.isEmpty(), "no journeys");

        validateStages(results);
    }

    @Test
    void shouldFindRouteAroundCloseBackOnToTramCornbrookToPicc() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                maxChanges, maxJourneyDuration, 1, getRequestedModes());
        Set<Journey> results = calculator.calculateRouteAsSet(Cornbrook, Piccadilly, journeyRequest);

        assertFalse(results.isEmpty(), "no journeys");

        validateStages(results);
    }

    @Test
    void shouldFindMonsallToDeansgate() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                maxChanges, maxJourneyDuration, 1, getRequestedModes());

        Set<Journey> results = calculator.calculateRouteAsSet(Monsall, Deansgate, journeyRequest);

        assertFalse(results.isEmpty(), "no journeys");

        results.forEach(result -> {
            final List<TransportStage<?, ?>> stages = result.getStages();
            assertEquals(2, stages.size(), "num stages " + result);
            TransportStage<?, ?> firstStage = stages.get(0);
            assertEquals(TransportMode.Tram, firstStage.getMode(), "1st mode " + result);
            assertEquals(ExchangeSquare.getId(), firstStage.getLastStation().getId());

            assertEquals(TransportMode.Connect, stages.get(1).getMode(), "2nd mode " + result);
        });
    }

    @Disabled("WIP")
    @Test
    void shouldFindRouteAroundCloseBackOnToTramMonsallToCornbrook() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                maxChanges, maxJourneyDuration, 1, getRequestedModes());

        journeyRequest.setDiag(true);

        Set<Journey> results = calculator.calculateRouteAsSet(Monsall, Cornbrook, journeyRequest);

        assertFalse(results.isEmpty(), "no journeys");

        validateStages(results);
    }

    @Test
    void shouldFindRouteAroundCloseBackOnToTramCornbrooktoMonsall() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                maxChanges, maxJourneyDuration, 1, getRequestedModes());

        Set<Journey> results = calculator.calculateRouteAsSet(Cornbrook, Monsall, journeyRequest);

        assertFalse(results.isEmpty(), "no journeys");

        validateStages(results);
    }

    private void validateStages(Set<Journey> results) {
        results.forEach(result -> {
            final List<TransportStage<?, ?>> stages = result.getStages();
            assertEquals(3, stages.size(), "num stages " + result);
            assertEquals(TransportMode.Tram, stages.get(0).getMode(), "1st mode " + result);
            assertEquals(TransportMode.Connect, stages.get(1).getMode(), "2nd mode " + result);
            assertEquals(TransportMode.Tram, stages.get(2).getMode(), "3rd mode " + result);
        });
    }

    @Test
    void shouldFindRouteToClosedStationViaWalkAtEnd() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8, 0),
                false, 3, maxJourneyDuration, 1, getRequestedModes());
        Set<Journey> results = calculator.calculateRouteAsSet(Cornbrook, TramStations.StPetersSquare, journeyRequest);

        assertFalse(results.isEmpty());

        results.forEach(result -> {
            final List<TransportStage<?, ?>> stages = result.getStages();
            assertEquals(2, stages.size(), "num stages " + result);
            assertEquals(TransportMode.Tram, stages.get(0).getMode(), "1st mode " + result);
            assertEquals(TransportMode.Connect, stages.get(1).getMode(), "2nd mode " + result);
        });
    }

    @Test
    void shouldFindRouteWhenFromStationWithDiversionToOtherDiversionStation() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                maxChanges, maxJourneyDuration, 1, getRequestedModes());
        Set<Journey> results = calculator.calculateRouteAsSet(ExchangeSquare, Deansgate, journeyRequest);

        assertFalse(results.isEmpty());
    }

    @Test
    void shouldCheckForExpectedInboundRelationships() {
        List<Long> foundRelationshipIds = new ArrayList<>();

        Station exchange = ExchangeSquare.from(stationRepository);
        GraphDatabase graphDatabase = componentContainer.get(GraphDatabase.class);
        GraphQuery graphQuery = componentContainer.get(GraphQuery.class);
        try (Transaction txn = graphDatabase.beginTx()) {
            exchange.getPlatforms().forEach(platform -> {
                Node node = graphQuery.getPlatformNode(txn, platform);
                Iterable<Relationship> iterable = node.getRelationships(Direction.INCOMING, TransportRelationshipTypes.DIVERSION_DEPART);

                iterable.forEach(relationship -> foundRelationshipIds.add(relationship.getId()));
            });

        }

        assertFalse(foundRelationshipIds.isEmpty());

        try (Transaction txn = graphDatabase.beginTx()) {
            Relationship relationship = txn.getRelationshipById(foundRelationshipIds.get(0));
            Node from = relationship.getStartNode();
            assertTrue(from.hasLabel(ROUTE_STATION), from.getAllProperties().toString());
            Node to = relationship.getEndNode();
            assertTrue(to.hasLabel(PLATFORM));
        }

    }

    @Test
    void produceDiagramOfGraphSubset() throws IOException {
        DiagramCreator creator = componentContainer.get(DiagramCreator.class);
        creator.create(Path.of("subgraph_central_with_closure_trams.dot"), StPetersSquare.fake(), 100, true);
    }

}

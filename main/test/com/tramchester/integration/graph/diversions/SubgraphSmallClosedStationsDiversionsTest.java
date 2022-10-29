package com.tramchester.integration.graph.diversions;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.DiagramCreator;
import com.tramchester.domain.*;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.dates.TramServiceDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.geo.StationLocations;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphQuery;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.filters.ConfigurableGraphFilter;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.graph.search.routes.RouteToRouteCosts;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.StationClosuresForTest;
import com.tramchester.integration.testSupport.tram.IntegrationTramClosedStationsTestConfig;
import com.tramchester.repository.ClosedStationsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.StationsWithDiversionRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.PiccGardens2022;
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
import java.util.stream.Collectors;

import static com.tramchester.graph.graphbuild.GraphLabel.PLATFORM;
import static com.tramchester.graph.graphbuild.GraphLabel.ROUTE_STATION;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

class SubgraphSmallClosedStationsDiversionsTest {
    // Note this needs to be > time for whole test fixture, see note below in @After
    private static final int TXN_TIMEOUT = 5*60;

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static IntegrationTramClosedStationsTestConfig config;

    private final static TramServiceDate when = new TramServiceDate(TestEnv.testDay());

    private final static List<StationClosures> closedStations = List.of(
            new StationClosuresForTest(PiccadillyGardens, when.getDate(), when.getDate().plusWeeks(1), false));

    private static final List<TramStations> centralStations = Arrays.asList(
            Cornbrook,
            Deansgate,
            StPetersSquare,
            ExchangeSquare,
            Victoria,
            PiccadillyGardens,
            Piccadilly,
            MarketStreet,
            Shudehill);
    
    private RouteCalculatorTestFacade calculator;
    private StationRepository stationRepository;
    private Transaction txn;
    private Duration maxJourneyDuration;
    private int maxChanges;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {
        config = new IntegrationTramClosedStationsTestConfig(closedStations, true);
        TestEnv.deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder().
                configureGraphFilter(SubgraphSmallClosedStationsDiversionsTest::configureFilter).
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
    void shouldValidateRangeIsCorrectForDiversions() {
        StationLocations stationLocations = componentContainer.get(StationLocations.class);

        MarginInMeters range = MarginInMeters.of(config.getNearestStopForWalkingRangeKM());

        Station piccGardens = PiccadillyGardens.from(stationRepository);

        Set<Station> withinRange = stationLocations.nearestStationsUnsorted(piccGardens, range).collect(Collectors.toSet());

        assertEquals(6, withinRange.size(), HasId.asIds(withinRange));

        // ignore self
        assertTrue(withinRange.contains(MarketStreet.from(stationRepository)));
        assertTrue(withinRange.contains(StPetersSquare.from(stationRepository)));
        assertTrue(withinRange.contains(Piccadilly.from(stationRepository)));
        assertTrue(withinRange.contains(Shudehill.from(stationRepository)));
        assertTrue(withinRange.contains(ExchangeSquare.from(stationRepository)));
    }

    @Test
    void shouldHaveTheDiversionsInTheRepository() {
        StationsWithDiversionRepository repository = componentContainer.get(StationsWithDiversionRepository.class);
        assertTrue(repository.hasDiversions(MarketStreet.from(stationRepository)));
        assertTrue(repository.hasDiversions(StPetersSquare.from(stationRepository)));
        assertTrue(repository.hasDiversions(Piccadilly.from(stationRepository)));
        assertTrue(repository.hasDiversions(Shudehill.from(stationRepository)));
        assertTrue(repository.hasDiversions(ExchangeSquare.from(stationRepository)));
    }

    @PiccGardens2022
    @Test
    void shouldHaveExpectedRouteToRouteCostsForClosedStations() {
        RouteToRouteCosts routeToRouteCosts = componentContainer.get(RouteToRouteCosts.class);

        Location<?> start = StPetersSquare.from(stationRepository);
        Location<?> destination = Piccadilly.from(stationRepository);

        TimeRange timeRange = TimeRange.of(TramTime.of(6,0), TramTime.of(23,55));
        Set<TransportMode> mode = EnumSet.of(TransportMode.Tram);

        NumberOfChanges costs = routeToRouteCosts.getNumberOfChanges(start, destination, mode, when.getDate().plusDays(1), timeRange);

        // note replacement bus is now available instead of the walk 1->0
        assertEquals(0, costs.getMin());
    }

    @Test
    void shouldHaveExpectedNeighboursForClosedPiccadillyGardens() {
        TramDate date = when.getDate();
        ClosedStationsRepository closedStationsRepository = componentContainer.get(ClosedStationsRepository.class);

        Station piccGardens = PiccadillyGardens.from(stationRepository);
        assertTrue(closedStationsRepository.isClosed(piccGardens, date));
        ClosedStation closed = closedStationsRepository.getClosedStation(piccGardens, date);

        Set<Station> nearby = closed.getNearbyLinkedStation();
        assertEquals(5, nearby.size(), HasId.asIds(nearby));
        assertTrue(nearby.contains(Piccadilly.from(stationRepository)));
        assertTrue(nearby.contains(MarketStreet.from(stationRepository)));
        assertTrue(nearby.contains(StPetersSquare.from(stationRepository)));
        assertTrue(nearby.contains(Shudehill.from(stationRepository)));
        assertTrue(nearby.contains(ExchangeSquare.from(stationRepository)));
    }

    @Test
    void shouldHaveJourneyFromPiccGardensToPiccadilly() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                maxChanges, maxJourneyDuration, 1, getRequestedModes());

        Set<Journey> results = calculator.calculateRouteAsSet(PiccadillyGardens, Victoria, journeyRequest);

        assertFalse(results.isEmpty(), "no journeys");
    }

    @Test
    void shouldFindRouteAroundCloseBackOnToTramCornbrookToPicc() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                maxChanges, maxJourneyDuration, 1, getRequestedModes());

        Set<Journey> results = calculator.calculateRouteAsSet(Cornbrook, Piccadilly, journeyRequest);

        assertFalse(results.isEmpty(), "no journeys");
    }

    @Test
    void shouldFindRouteAroundCloseBackOnToTramPiccToCornbrook() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                maxChanges, maxJourneyDuration, 1, getRequestedModes());

        Set<Journey> results = calculator.calculateRouteAsSet(Piccadilly, Cornbrook, journeyRequest);

        assertFalse(results.isEmpty(), "no journeys");
    }

    @Test
    void shouldFindRouteAroundCloseBackOnToTramVicToPicc() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                maxChanges, maxJourneyDuration, 1, getRequestedModes());

        Set<Journey> results = calculator.calculateRouteAsSet(Victoria, Piccadilly, journeyRequest);

        assertFalse(results.isEmpty(), "no journeys");
    }

    @Test
    void shouldFindRouteAroundCloseBackOnToTramPiccToVic() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                maxChanges, maxJourneyDuration, 1, getRequestedModes());

        Set<Journey> results = calculator.calculateRouteAsSet(Piccadilly, Victoria, journeyRequest);

        assertFalse(results.isEmpty(), "no journeys");
    }

    @Test
    void shouldFindRouteWhenFromStationWithDiversionToOtherDiversionStation() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                maxChanges, maxJourneyDuration, 1, getRequestedModes());
        Set<Journey> results = calculator.calculateRouteAsSet(ExchangeSquare, Deansgate, journeyRequest);

        assertFalse(results.isEmpty());
    }

    @Test
    void shouldFindPiccadillyToPiccadillyGardens() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                maxChanges, maxJourneyDuration, 1, getRequestedModes());
        Set<Journey> results = calculator.calculateRouteAsSet(Piccadilly, PiccadillyGardens, journeyRequest);

        assertFalse(results.isEmpty());
    }

    @Test
    void shouldFindStPetersToPiccadillyGardens() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                maxChanges, maxJourneyDuration, 1, getRequestedModes());
        Set<Journey> results = calculator.calculateRouteAsSet(StPetersSquare, PiccadillyGardens, journeyRequest);

        assertFalse(results.isEmpty());
    }

    @Test
    void shouldFindDeansgateToPiccadillyGardens() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                maxChanges, maxJourneyDuration, 1, getRequestedModes());

        Set<Journey> results = calculator.calculateRouteAsSet(Deansgate, PiccadillyGardens, journeyRequest);

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

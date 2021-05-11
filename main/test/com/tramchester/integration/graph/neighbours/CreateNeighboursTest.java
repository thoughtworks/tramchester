package com.tramchester.integration.graph.neighbours;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.CompositeStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.CreateNeighbours;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphQuery;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.graph.testSupport.RouteCalculatorTestFacade;
import com.tramchester.repository.CompositeStationRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Bus;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.graph.TransportRelationshipTypes.NEIGHBOUR;
import static com.tramchester.testSupport.reference.TramStations.Bury;
import static com.tramchester.testSupport.reference.TramStations.Shudehill;
import static org.junit.jupiter.api.Assertions.*;
import static org.neo4j.graphdb.Direction.INCOMING;
import static org.neo4j.graphdb.Direction.OUTGOING;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class CreateNeighboursTest {

    private StationRepository stationRepository;
    private static RouteCalculatorTestFacade routeCalculator;
    private static TramchesterConfig config;
    private CompositeStation shudehillBus;
    private Station shudehillTram;

    private static ComponentContainer componentContainer;
    private GraphQuery graphQuery;
    private Transaction txn;
    private CreateNeighbours createNeighbours;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {
        config = new NeighboursTestConfig();
        //TestEnv.deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTest() {
        GraphDatabase graphDatabase = componentContainer.get(GraphDatabase.class);
        stationRepository = componentContainer.get(StationRepository.class);
        CompositeStationRepository compositeStationRepository = componentContainer.get(CompositeStationRepository.class);
        graphQuery = componentContainer.get(GraphQuery.class);
        shudehillBus = compositeStationRepository.findByName("Shudehill Interchange");
        shudehillTram = compositeStationRepository.getStationById(Shudehill.getId());

        txn = graphDatabase.beginTx();
        routeCalculator = new RouteCalculatorTestFacade(componentContainer.get(RouteCalculator.class), stationRepository, txn);

        // force creation and init
        createNeighbours = componentContainer.get(CreateNeighbours.class);
    }

    @Test
    void shouldFindTheBusStation() {
        assertNotNull(shudehillBus);
    }

    @AfterEach
    void onceAfterEachTestHasRun() {
        txn.close();
    }

    @Test
    void shouldHaveExpectedNeighbourRelationshipsToFromTram() {
        Node tramNode = graphQuery.getStationNode(txn, shudehillTram);
        assertNotNull(tramNode);

        Station victoria = TramStations.of(TramStations.Victoria);

        Set<Relationship> awayFrom = getRelationships(tramNode, OUTGOING);

        assertTrue(seenNode(txn, shudehillBus, awayFrom, Relationship::getEndNode));
        assertFalse(seenNode(txn, victoria, awayFrom, Relationship::getEndNode));
        assertFalse(seenNode(txn, shudehillTram, awayFrom, Relationship::getEndNode));

        Set<Relationship> towards = getRelationships(tramNode, INCOMING);
        assertTrue(seenNode(txn, shudehillBus, towards, Relationship::getStartNode));
        assertFalse(seenNode(txn, victoria, towards, Relationship::getStartNode));
    }

    @Test
    void shouldHaveExpectedNeighbourRelationshipsToFromBus() {
        Node busNode = graphQuery.getStationOrGrouped(txn, shudehillBus);
        assertNotNull(busNode, "No node found for " + shudehillBus);

        Set<Relationship> awayFrom = getRelationships(busNode, OUTGOING);
        assertTrue(seenNode(txn, shudehillTram, awayFrom, Relationship::getEndNode));
        assertFalse(seenNode(txn, shudehillBus, awayFrom, Relationship::getEndNode));

        Set<Relationship> towards = getRelationships(busNode, INCOMING);
        assertTrue(seenNode(txn, shudehillTram, towards, Relationship::getStartNode));
        assertFalse(seenNode(txn, shudehillBus, towards, Relationship::getStartNode));
    }

    @Test
    void shouldHaveListOfStationsThatNowHaveNeighboursBus() {
        IdSet<Station> haveNeighboursBus = createNeighbours.getStationsWithNeighbours(Bus);
        assertEquals(258, haveNeighboursBus.size());

        assertTrue(haveNeighboursBus.contains(BusStations.StopAtAltrinchamInterchange.getId()));
        assertFalse(haveNeighboursBus.contains(shudehillTram.getId()));
        assertFalse(haveNeighboursBus.contains(BusStations.KnutsfordStationStand3.getId()));

    }

    @Test
    void shouldHaveListOfStationsThatNowHaveNeighboursTram() {
        IdSet<Station> haveNeighboursTram = createNeighbours.getStationsWithNeighbours(Tram);
        assertTrue(haveNeighboursTram.contains(shudehillTram.getId()));
        assertFalse(haveNeighboursTram.contains(shudehillBus.getId()));
        assertFalse(haveNeighboursTram.contains(BusStations.KnutsfordStationStand3.getId()));
    }

    private Set<Relationship> getRelationships(Node node, Direction direction) {
        Set<Relationship> result = new HashSet<>();
        Iterable<Relationship> iter = node.getRelationships(direction, NEIGHBOUR);
        iter.forEach(result::add);
        return result;
    }

    @Test
    void shouldDirectWalkIfStationIsNeighbourTramToBus() {
        validateDirectWalk(shudehillTram, shudehillBus);
    }

    @Test
    void shouldDirectWalkIfStationIsNeighbourBusToTram() {
        validateDirectWalk(shudehillBus, shudehillTram);
    }

    @Test
    void shouldTramNormally() {

        JourneyRequest request = new JourneyRequest(new TramServiceDate(TestEnv.testDay()),
                TramTime.of(11,53), false, 8, config.getMaxJourneyDuration());

        Set<Journey> journeys = routeCalculator.calculateRouteAsSet(Bury, Shudehill, request);

        assertFalse(journeys.isEmpty());
        journeys.forEach(journey -> {
            assertEquals(1, journey.getStages().size(), journey.toString());
            TransportStage<?,?> stage = journey.getStages().get(0);
            assertEquals(Tram, stage.getMode());
        });
    }

    @Test
    void shouldTramThenWalk() {

        JourneyRequest request = new JourneyRequest(new TramServiceDate(TestEnv.testDay()),
                TramTime.of(11,53), false, 0, config.getMaxJourneyDuration());

        Station startStation = stationRepository.getStationById(Bury.getId());
        Set<Journey> allJourneys = routeCalculator.calculateRouteAsSet(startStation, shudehillBus, request);

        Set<Journey> maybeTram = allJourneys.stream().filter(journey -> journey.getStages().size()<=2).collect(Collectors.toSet());
        assertFalse(maybeTram.isEmpty());

        maybeTram.forEach(journey -> {
            TransportStage<?,?> first = journey.getStages().get(0);
            assertEquals(Tram, first.getMode());
            TransportStage<?,?> second = journey.getStages().get(1);
            assertEquals(TransportMode.Connect, second.getMode());
        });
    }

    private void validateDirectWalk(Station start, Station end) {

        JourneyRequest request =
                new JourneyRequest(new TramServiceDate(TestEnv.testDay()), TramTime.of(11,45),
                        false, 0, config.getMaxJourneyDuration());

        Set<Journey> journeys =  routeCalculator.calculateRouteAsSet(start, end, request);

        assertFalse(journeys.isEmpty());
        journeys.forEach(journey -> {
            assertEquals(1, journey.getStages().size(), journey.toString());
            TransportStage<?,?> stage = journey.getStages().get(0);
            assertEquals(TransportMode.Connect, stage.getMode());
        });
    }

    private boolean seenNode(Transaction txn, Station station, Set<Relationship> relationships, SelectNode selectNode) {
        Node nodeToFind = graphQuery.getStationOrGrouped(txn, station);
        assertNotNull(nodeToFind, "no node found for " + station);
        boolean seenNode = false;
        for (Relationship relationship : relationships) {
            if (selectNode.getNode(relationship).getId()==nodeToFind.getId()) {
                seenNode = true;
                break;
            }
        }
        return seenNode;
    }

    private interface SelectNode {
        Node getNode(Relationship relationship);
    }

}

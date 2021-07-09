package com.tramchester.integration.graph.neighbours;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.CompositeStation;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.CreateNeighbours;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphQuery;
import com.tramchester.graph.graphbuild.CompositeStationGraphBuilder;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.integration.testSupport.NeighboursTestConfig;
import com.tramchester.repository.CompositeStationRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.neo4j.graphdb.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Bus;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.graph.TransportRelationshipTypes.NEIGHBOUR;
import static com.tramchester.integration.repository.TransportDataFromFilesTramTest.NUM_TFGM_TRAM_STATIONS;
import static com.tramchester.testSupport.reference.TramStations.Shudehill;
import static org.junit.jupiter.api.Assertions.*;
import static org.neo4j.graphdb.Direction.INCOMING;
import static org.neo4j.graphdb.Direction.OUTGOING;

@BusTest
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class CreateNeighboursTest {

    private static GraphDatabase graphDatabase;
    private CompositeStation shudehillCompositeBus;
    private Station shudehillTram;

    private static ComponentContainer componentContainer;
    private GraphQuery graphQuery;
    private Transaction txn;
    private CreateNeighbours createNeighbours;
    private CompositeStationRepository compositeStationRepository;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig config = new NeighboursTestConfig();

        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
        graphDatabase = componentContainer.get(GraphDatabase.class);

        // make sure composites added to the DB
        CompositeStationGraphBuilder builder = componentContainer.get(CompositeStationGraphBuilder.class);
        builder.getReady();

        // force init of main DB and hence save of VERSION node, so avoid multiple rebuilds of the DB
        componentContainer.get(StagedTransportGraphBuilder.Ready.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTest() {
        stationRepository = componentContainer.get(StationRepository.class);

        compositeStationRepository = componentContainer.get(CompositeStationRepository.class);

        graphQuery = componentContainer.get(GraphQuery.class);

        shudehillCompositeBus = compositeStationRepository.findByName("Shudehill Interchange");
        shudehillTram = compositeStationRepository.getStationById(Shudehill.getId());

        // force creation and init
        createNeighbours = componentContainer.get(CreateNeighbours.class);

        txn = graphDatabase.beginTx();
    }

    @AfterEach
    void onceAfterEachTestHasRun() {
        txn.close();
    }

    @Test
    void shouldHaveExpectedNumbersForStations() {

        long busOnly = stationRepository.getStationStream().
                filter(station -> !station.isComposite()).
                filter(station -> (station.serves(Bus) && !station.serves(Tram))).count();

        assertEquals(NUM_TFGM_TRAM_STATIONS, countNodes(GraphLabel.TRAM_STATION));
        assertEquals(busOnly, countNodes(GraphLabel.BUS_STATION));
    }

    private int countNodes(GraphLabel graphLabel) {
        ResourceIterator<Node> tramNodes = graphDatabase.findNodes(txn, graphLabel);
        int count = 0;
        while (tramNodes.hasNext()) {
            tramNodes.next();
            count++;
        }
        return count;
    }

    @Test
    void shouldFindTheCompositeBusStation() {
        assertNotNull(shudehillCompositeBus);
        Node compNode = graphQuery.getStationOrGrouped(txn, shudehillCompositeBus);
        assertNotNull(compNode, "No node found for " + compNode);
        shudehillCompositeBus.getContained().forEach(busStop -> {
            Node busNode = graphQuery.getStationOrGrouped(txn, busStop);
            assertNotNull(busNode, "No node found for " + busStop);
        });
    }

    @Test
    void shouldHaveTramStationForTesting() {
        assertNotNull(shudehillTram);
        Node stationNode = graphQuery.getStationNode(txn, shudehillTram);
        assertNotNull(stationNode);
    }

    @Test
    void shouldHaveAllTramStations() {
        Set<Station> missingNodes = Arrays.stream(TramStations.values()).
                map(TramStations::getId).
                map(id -> compositeStationRepository.getStationById(id)).
                filter(station -> graphQuery.getStationNode(txn, station) == null).
                collect(Collectors.toSet());
        assertEquals(Collections.emptySet(), missingNodes);
    }

    @Test
    void shouldHaveExpectedNeighbourRelationshipsToFromTram() {
        Node tramNode = graphQuery.getStationNode(txn, shudehillTram);
        assertNotNull(tramNode);

        Station victoria = TramStations.of(TramStations.Victoria);

        Set<Relationship> outFromTram = getRelationships(tramNode, OUTGOING);
        Set<Relationship> towardsTram = getRelationships(tramNode, INCOMING);

        assertFalse(seenNode(txn, victoria, outFromTram, Relationship::getEndNode));
        assertFalse(seenNode(txn, shudehillTram, outFromTram, Relationship::getEndNode));
        assertFalse(seenNode(txn, victoria, towardsTram, Relationship::getStartNode));

        shudehillCompositeBus.getContained().forEach(busStop -> {
            assertTrue(seenNode(txn, busStop, outFromTram, Relationship::getEndNode));
            assertTrue(seenNode(txn, busStop, towardsTram, Relationship::getStartNode));
        });

    }

    @Test
    void shouldHaveExpectedNeighbourRelationshipsToFromBus() {

        shudehillCompositeBus.getContained().forEach(busStop -> {
            Node busNode = graphQuery.getStationOrGrouped(txn, busStop);
            assertNotNull(busNode, "No node found for " + busStop);

            Set<Relationship> awayFrom = getRelationships(busNode, OUTGOING);
            assertTrue(seenNode(txn, shudehillTram, awayFrom, Relationship::getEndNode));

            Set<Relationship> towards = getRelationships(busNode, INCOMING);
            assertTrue(seenNode(txn, shudehillTram, towards, Relationship::getStartNode));
        });

    }

    @Test
    void shouldHaveCorrectNeighboursForAltrinchamTram() {
        CompositeStation altrinchamComposite = compositeStationRepository.findByName("Altrincham Interchange");

        IdSet<Station> neighbours = createNeighbours.getNeighboursFor(TramStations.Altrincham.getId());
        IdSet<Station> ids = altrinchamComposite.getContained().stream().collect(IdSet.collector());

        assertTrue(neighbours.containsAll(ids));
    }

    @Test
    void shouldHaveCorrectNeighboursForTramAtShudehill() {
        IdSet<Station> neighbours = createNeighbours.getNeighboursFor(Shudehill.getId());
        IdSet<Station> busStops = shudehillCompositeBus.getContained().stream().collect(IdSet.collector());
        assertTrue(neighbours.containsAll(busStops));
    }

    @Test
    void shouldHaveCorrectNeighboursForBusAtShudehill() {
        shudehillCompositeBus.getContained().forEach(station -> {
            IdSet<Station> neighbours = createNeighbours.getNeighboursFor(station.getId());
            assertTrue(neighbours.contains(shudehillTram.getId()));
        });
    }

    private Set<Relationship> getRelationships(Node node, Direction direction) {
        Set<Relationship> result = new HashSet<>();
        Iterable<Relationship> iter = node.getRelationships(direction, NEIGHBOUR);
        iter.forEach(result::add);
        return result;
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

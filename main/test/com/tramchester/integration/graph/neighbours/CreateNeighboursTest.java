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
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.integration.testSupport.NeighboursTestConfig;
import com.tramchester.repository.CompositeStationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.util.HashSet;
import java.util.Set;

import static com.tramchester.graph.TransportRelationshipTypes.NEIGHBOUR;
import static com.tramchester.testSupport.reference.TramStations.Shudehill;
import static org.junit.jupiter.api.Assertions.*;
import static org.neo4j.graphdb.Direction.INCOMING;
import static org.neo4j.graphdb.Direction.OUTGOING;

@BusTest
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class CreateNeighboursTest {

    private CompositeStation shudehillCompositeBus;
    private Station shudehillTram;

    private static ComponentContainer componentContainer;
    private GraphQuery graphQuery;
    private Transaction txn;
    private CreateNeighbours createNeighbours;
    private CompositeStationRepository compositeStationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig config = new NeighboursTestConfig();

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
        compositeStationRepository = componentContainer.get(CompositeStationRepository.class);

        graphQuery = componentContainer.get(GraphQuery.class);
        shudehillCompositeBus = compositeStationRepository.findByName("Shudehill Interchange");
        shudehillTram = compositeStationRepository.getStationById(Shudehill.getId());

        txn = graphDatabase.beginTx();

        // force creation and init
        createNeighbours = componentContainer.get(CreateNeighbours.class);

        // force init of main DB and hence save of VERSION node, so avoid multiple rebuilds of the DB
        componentContainer.get(StagedTransportGraphBuilder.class);
    }

    @AfterEach
    void onceAfterEachTestHasRun() {
        txn.close();
    }

    @Test
    void shouldFindTheCompositeBusStation() {
        assertNotNull(shudehillCompositeBus);
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

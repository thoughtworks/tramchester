package com.tramchester.integration.graph.neighbours;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphQuery;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.integration.testSupport.NeighboursTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;

import java.util.Set;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.integration.repository.TransportDataFromFilesTramTest.NUM_TFGM_TRAM_STATIONS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@BusTest
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class NeighboursGraphBuilderTest {

    private static GraphDatabase graphDatabase;

    private static ComponentContainer componentContainer;
    private Transaction txn;
    private StationRepository stationRepository;

    /// Not if neighbours added, just if graph built with both Bus and Tram nodes correctly

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig config = new NeighboursTestConfig();

        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        // force init of main DB and hence save of VERSION node, so avoid multiple rebuilds of the DB
        componentContainer.get(StagedTransportGraphBuilder.class);

        graphDatabase = componentContainer.get(GraphDatabase.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTest() {
        stationRepository = componentContainer.get(StationRepository.class);

        txn = graphDatabase.beginTx();
    }

    @AfterEach
    void onceAfterEachTestHasRun() {
        txn.close();
    }

    @Test
    void shouldHaveExpectedNumberForTramStations() {
        assertEquals(NUM_TFGM_TRAM_STATIONS, countNodes(GraphLabel.TRAM_STATION));
    }

    @Test
    void shouldHaveNodesForAllStations() {
        final Set<Station> stations = stationRepository.getStationsServing(Tram);
        long tram = stations.size();
        assertEquals(NUM_TFGM_TRAM_STATIONS, tram);

        GraphQuery graphQuery = componentContainer.get(GraphQuery.class);
        stations.forEach(station ->
                assertNotNull(graphQuery.getStationNode(txn, station), station.getId() + " is missing from DB"));
    }

    @Test
    void shouldHaveExpectedNumbersForBusStations() {

        long busStations = stationRepository.getNumberOfStations()-NUM_TFGM_TRAM_STATIONS;

        assertEquals(busStations, countNodes(GraphLabel.BUS_STATION));
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

}

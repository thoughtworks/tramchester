package com.tramchester.integration.graph.trains;

import com.google.common.collect.Lists;
import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphQuery;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.TrainTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.graph.TransportRelationshipTypes.LINKED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TrainTest
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class GraphBuilderTrainTest {
    private static ComponentContainer componentContainer;

    private Transaction txn;
    private GraphQuery graphQuery;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig testConfig = new IntegrationRailTestConfig();
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        graphQuery = componentContainer.get(GraphQuery.class);
        stationRepository = componentContainer.get(StationRepository.class);
        GraphDatabase service = componentContainer.get(GraphDatabase.class);
        txn = service.beginTx();
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @Test
    void shouldHaveLinkRelationshipsCorrectForNonInterchange() {
        Station mobberley = stationRepository.getStationById(RailStationIds.Mobberley.getId());
        Node mobberleyNode = graphQuery.getStationNode(txn, mobberley);
        Iterable<Relationship> outboundLinks = mobberleyNode.getRelationships(Direction.OUTGOING, LINKED);

        List<Relationship> list = Lists.newArrayList(outboundLinks);
        assertEquals(2, list.size(), list.toString());

        Set<IdFor<Station>> destinations = list.stream().map(Relationship::getEndNode).
                map(GraphProps::getStationId).collect(Collectors.toSet());

        assertTrue(destinations.contains(RailStationIds.Knutsford.getId()));
    }

    @Test
    void shouldHaveLinkRelationshipsCorrectForInterchange() {
        Station station = stationRepository.getStationById(RailStationIds.ManchesterPiccadilly.getId());
        Node manPiccNode = graphQuery.getStationNode(txn, station);
        Iterable<Relationship> outboundLinks = manPiccNode.getRelationships(Direction.OUTGOING, LINKED);

        List<Relationship> list = Lists.newArrayList(outboundLinks);
        assertEquals(14, list.size());

        Set<IdFor<Station>> destinations = list.stream().map(Relationship::getEndNode).
                map(GraphProps::getStationId).collect(Collectors.toSet());

        assertTrue(destinations.contains(RailStationIds.Stockport.getId()));
    }

}

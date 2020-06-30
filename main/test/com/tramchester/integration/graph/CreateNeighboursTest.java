package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.StationLocationsRepository;
import com.tramchester.graph.CreateNeighbours;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphQuery;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.IntegrationBusTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.BusStations;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestEnv;
import junit.extensions.TestSetup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class CreateNeighboursTest {

    private static Dependencies dependencies;
    private static GraphDatabase database;
    private GraphQuery graphQuery;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        TramchesterConfig testConfig = new IntegrationBusTestConfig();
        dependencies.initialise(testConfig);
        database = dependencies.get(GraphDatabase.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        graphQuery = dependencies.get(GraphQuery.class);
    }

    @Test
    void shouldCreateNeighbourRelationships() {
        StationRepository repository = dependencies.get(StationRepository.class);
        StationLocationsRepository stationLocations = dependencies.get(StationLocationsRepository.class);
        double rangeInKM = 0.4D;

        CreateNeighbours createNeighbours = new CreateNeighbours(database, graphQuery, repository, stationLocations, rangeInKM);

        try(Transaction txn = database.beginTx()) {
            createNeighbours.buildWithNoCommit(txn);

            Node shudehillTramNode = graphQuery.getStationNode(txn, Stations.Shudehill);
            Iterable<Relationship> outbounds = shudehillTramNode.getRelationships(Direction.OUTGOING, TransportRelationshipTypes.NEIGHBOUR);
            assertTrue(seenNode(txn, BusStations.ShudehillInterchange, outbounds, Relationship::getEndNode));
            assertTrue(seenNode(txn, Stations.Victoria, outbounds,  Relationship::getEndNode));

            Iterable<Relationship> inbounds = shudehillTramNode.getRelationships(Direction.INCOMING, TransportRelationshipTypes.NEIGHBOUR);
            assertTrue(seenNode(txn, BusStations.ShudehillInterchange, inbounds, Relationship::getStartNode));
            assertTrue(seenNode(txn, Stations.Victoria, inbounds, Relationship::getStartNode));
        }
    }

    @Test
    void shouldSuggestDirectionWalkIfStationIsNeighbour() {
        try(Transaction txn = database.beginTx()) {
            RouteCalculator routeCalculator = dependencies.get(RouteCalculator.class);
            JourneyRequest request = new JourneyRequest(new TramServiceDate(TestEnv.testDay()), TramTime.of(11,45), false);
            Stream<Journey> stream = routeCalculator.calculateRoute(txn, Stations.Shudehill, BusStations.ShudehillInterchange, request);

            Set<Journey> journeys = stream.collect(Collectors.toSet());
            assertFalse(journeys.isEmpty());
            journeys.forEach(journey -> {
                assertEquals(1, journey.getStages().size());
                TransportStage stage = journey.getStages().get(0);
                assertEquals(TransportMode.Walk, stage.getMode());
            });
        }
    }

    private boolean seenNode(Transaction txn, Station station, Iterable<Relationship> outbounds, SelectNode selectNode) {
        Node nodeToFind = graphQuery.getStationNode(txn, station);
        boolean seenNode = false;
        for (Relationship relationship : outbounds) {
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

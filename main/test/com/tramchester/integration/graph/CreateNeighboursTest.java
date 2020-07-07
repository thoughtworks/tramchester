package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
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
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@Disabled("WIP")
class CreateNeighboursTest {

    private static Dependencies dependencies;
    private static GraphDatabase database;
    private static IntegrationBusTestConfig testConfig;
    private GraphQuery graphQuery;
    private CreateNeighbours createNeighbours;
    private Transaction txn;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        testConfig = new IntegrationBusTestConfig();
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
        StationRepository repository = dependencies.get(StationRepository.class);
        StationLocationsRepository stationLocations = dependencies.get(StationLocationsRepository.class);

        createNeighbours = new CreateNeighbours(database, graphQuery, repository, stationLocations, testConfig);

        txn = database.beginTx();
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.rollback();
    }

    @Test
    void shouldCreateNeighbourRelationships() {

        createNeighbours.buildWithNoCommit(txn);

        Node shudehillTramNode = graphQuery.getStationNode(txn, Stations.Shudehill);
        Iterable<Relationship> busOutbounds = shudehillTramNode.getRelationships(Direction.OUTGOING, TransportRelationshipTypes.BUS_NEIGHBOUR);
        assertTrue(seenNode(txn, BusStations.ShudehillInterchange, busOutbounds, Relationship::getEndNode));

        Iterable<Relationship> tramOutbounds = shudehillTramNode.getRelationships(Direction.OUTGOING, TransportRelationshipTypes.TRAM_NEIGHBOUR);
        assertTrue(seenNode(txn, Stations.Victoria, tramOutbounds,  Relationship::getEndNode));

        Iterable<Relationship> inbounds = shudehillTramNode.getRelationships(Direction.INCOMING, TransportRelationshipTypes.TRAM_NEIGHBOUR);
        assertTrue(seenNode(txn, BusStations.ShudehillInterchange, inbounds, Relationship::getStartNode));
        assertTrue(seenNode(txn, Stations.Victoria, inbounds, Relationship::getStartNode));
    }

    @Test
    void shouldDirectWalkIfStationIsNeighbourTramToBus() {
        validateDirectWalk(Stations.Shudehill, BusStations.ShudehillInterchange);
    }

    @Test
    void shouldDirectWalkIfStationIsNeighbourBusToTram() {
        validateDirectWalk(BusStations.ShudehillInterchange, Stations.Shudehill);
    }

    @Test
    void shouldTramNormally() {
        createNeighbours.buildWithNoCommit(txn);

        RouteCalculator routeCalculator = dependencies.get(RouteCalculator.class);
        JourneyRequest request = new JourneyRequest(new TramServiceDate(TestEnv.testDay()),
                TramTime.of(11,53), false, 8, testConfig.getMaxJourneyDuration());
        //request.setDiag(true);

        Station startStation = Stations.Bury;
        Station end = Stations.Shudehill;
        Stream<Journey> stream = routeCalculator.calculateRoute(txn, startStation, end, request);

        Set<Journey> journeys = stream.collect(Collectors.toSet());
        assertFalse(journeys.isEmpty());
        journeys.forEach(journey -> {
            assertEquals(1, journey.getStages().size(), journey.toString());
            TransportStage stage = journey.getStages().get(0);
            assertEquals(TransportMode.Tram, stage.getMode());
        });
    }

    @Test
    void shouldTramThenWalk() {
        createNeighbours.buildWithNoCommit(txn);

        RouteCalculator routeCalculator = dependencies.get(RouteCalculator.class);
        JourneyRequest request = new JourneyRequest(new TramServiceDate(TestEnv.testDay()),
                TramTime.of(11,53), false, 0, testConfig.getMaxJourneyDuration());
        request.setDiag(true);

        Station startStation = Stations.Bury;
        Station end = BusStations.ShudehillInterchange;
        Set<Journey> allJourneys = routeCalculator.calculateRoute(txn, startStation, end, request).collect(Collectors.toSet());

        Set<Journey> maybeTram = allJourneys.stream().filter(journey -> journey.getStages().size()<=2).collect(Collectors.toSet());
        assertFalse(maybeTram.isEmpty());

        maybeTram.forEach(journey -> {
            TransportStage first = journey.getStages().get(0);
            assertEquals(TransportMode.Tram, first.getMode());
            TransportStage second = journey.getStages().get(1);
            assertEquals(TransportMode.Connect, second.getMode());
        });
    }

    private void validateDirectWalk(Station startStation, Station end) {
        createNeighbours.buildWithNoCommit(txn);

        RouteCalculator routeCalculator = dependencies.get(RouteCalculator.class);
        JourneyRequest request =
                new JourneyRequest(new TramServiceDate(TestEnv.testDay()), TramTime.of(11,45),
                        false, 0, testConfig.getMaxJourneyDuration());

        Stream<Journey> stream = routeCalculator.calculateRoute(txn, startStation, end, request);

        Set<Journey> journeys = stream.collect(Collectors.toSet());
        assertFalse(journeys.isEmpty());
        journeys.forEach(journey -> {
            assertEquals(1, journey.getStages().size(), journey.toString());
            TransportStage stage = journey.getStages().get(0);
            assertEquals(TransportMode.Connect, stage.getMode());
        });
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

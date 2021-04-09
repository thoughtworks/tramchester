package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.CreateNeighbours;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphQuery;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.graph.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.GraphDBTestConfig;
import com.tramchester.integration.testSupport.IntegrationTestConfig;
import com.tramchester.integration.testSupport.tfgm.TFGMGTFSSourceTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TestStations;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Bus;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static org.junit.jupiter.api.Assertions.*;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class CreateNeighboursTest {

    private StationRepository stationRepository;
    private static RouteCalculatorTestFacade routeCalculator;
    private static NeighboursTestConfig testConfig;

    static class NeighboursTestConfig extends IntegrationTestConfig {
        public NeighboursTestConfig() {
            super(
                    new GraphDBTestConfig("integrationNeighboursTest", "neighboursBusAndTram.db"));
        }

        @Override
        protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
            return Collections.singletonList(
                    new TFGMGTFSSourceTestConfig("data/neighbours", TestEnv.tramAndBus,
                            new HashSet<>(Arrays.asList(Tram, Bus))));
        }

        @Override
        public boolean getCreateNeighbours() {
            return true;
        }
    }

    private static ComponentContainer componentContainer;
    private GraphQuery graphQuery;
    private Transaction txn;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        testConfig = new NeighboursTestConfig();

        componentContainer = new ComponentsBuilder<>().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        // force creation and init
        componentContainer.get(CreateNeighbours.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTest() {
        GraphDatabase graphDatabase = componentContainer.get(GraphDatabase.class);
        stationRepository = componentContainer.get(StationRepository.class);
        graphQuery = componentContainer.get(GraphQuery.class);

        txn = graphDatabase.beginTx();
        routeCalculator = new RouteCalculatorTestFacade(componentContainer.get(RouteCalculator.class), stationRepository, txn);
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldHaveExpectedNeighbourRelationships() {

        Node shudehillTramNode = graphQuery.getStationNode(txn, TramStations.of(TramStations.Shudehill));
        Iterable<Relationship> busOutbounds = shudehillTramNode.getRelationships(Direction.OUTGOING, TransportRelationshipTypes.NEIGHBOUR);
        assertTrue(seenNode(txn, BusStations.ShudehillInterchange, busOutbounds, Relationship::getEndNode));

        Iterable<Relationship> tramOutbounds = shudehillTramNode.getRelationships(Direction.OUTGOING, TransportRelationshipTypes.NEIGHBOUR);
        assertTrue(seenNode(txn, TramStations.of(TramStations.Victoria), tramOutbounds,  Relationship::getEndNode));

        Iterable<Relationship> inbounds = shudehillTramNode.getRelationships(Direction.INCOMING, TransportRelationshipTypes.NEIGHBOUR);
        assertTrue(seenNode(txn, BusStations.ShudehillInterchange, inbounds, Relationship::getStartNode));
        assertTrue(seenNode(txn, TramStations.of(TramStations.Victoria), inbounds, Relationship::getStartNode));
    }

    @Test
    void shouldDirectWalkIfStationIsNeighbourTramToBus() {
        validateDirectWalk(TramStations.Shudehill, BusStations.ShudehillInterchange);
    }

    @Test
    void shouldDirectWalkIfStationIsNeighbourBusToTram() {
        validateDirectWalk(BusStations.ShudehillInterchange, TramStations.Shudehill);
    }

    @Test
    void shouldTramNormally() {

        JourneyRequest request = new JourneyRequest(new TramServiceDate(TestEnv.testDay()),
                TramTime.of(11,53), false, 8, testConfig.getMaxJourneyDuration());
        //request.setDiag(true);

        TramStations startStation = TramStations.Bury;
        TramStations end = TramStations.Shudehill;
        Set<Journey> journeys = routeCalculator.calculateRouteAsSet(startStation, end, request);

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
                TramTime.of(11,53), false, 0, testConfig.getMaxJourneyDuration());
        //request.setDiag(true);

        TramStations startStation = TramStations.Bury;
        BusStations end = BusStations.ShudehillInterchange;
        Set<Journey> allJourneys = routeCalculator.calculateRouteAsSet(startStation, end, request);

        Set<Journey> maybeTram = allJourneys.stream().filter(journey -> journey.getStages().size()<=2).collect(Collectors.toSet());
        assertFalse(maybeTram.isEmpty());

        maybeTram.forEach(journey -> {
            TransportStage<?,?> first = journey.getStages().get(0);
            assertEquals(Tram, first.getMode());
            TransportStage<?,?> second = journey.getStages().get(1);
            assertEquals(TransportMode.Connect, second.getMode());
        });
    }

    private void validateDirectWalk(TestStations start, TestStations end) {

        JourneyRequest request =
                new JourneyRequest(new TramServiceDate(TestEnv.testDay()), TramTime.of(11,45),
                        false, 0, testConfig.getMaxJourneyDuration());

        Set<Journey> journeys =  routeCalculator.calculateRouteAsSet(start, end, request);

        assertFalse(journeys.isEmpty());
        journeys.forEach(journey -> {
            assertEquals(1, journey.getStages().size(), journey.toString());
            TransportStage<?,?> stage = journey.getStages().get(0);
            assertEquals(TransportMode.Connect, stage.getMode());
        });
    }

    private boolean seenNode(Transaction txn, BusStations station, Iterable<Relationship> outbounds, SelectNode selectNode) {
        return seenNode(txn, BusStations.real(stationRepository, station), outbounds, selectNode);
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

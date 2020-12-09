package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.DataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.StationLocationsRepository;
import com.tramchester.graph.CreateNeighbours;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphQuery;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.graphbuild.IncludeAllFilter;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.testSupport.IntegrationTestConfig;
import com.tramchester.integration.testSupport.TFGMTestDataSourceConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.*;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class CreateNeighboursTest {

    private static StationRepository stationRepository;
    private static RouteCalculatorTestFacade routeCalculator;

    static class NeighboursTestConfig extends IntegrationTestConfig {
        public NeighboursTestConfig() {
            super("integrationNeighboursTest", "busAndTram.db");
        }

        @Override
        protected List<DataSourceConfig> getDataSourceFORTESTING() {
            return Collections.singletonList(new TFGMTestDataSourceConfig("data/neighbours", TestEnv.tramAndBus));
        }

        @Override
        public boolean getCreateNeighbours() {
            return true;
        }
    }

    private static ComponentContainer componentContainer;
    private static TramchesterConfig testConfig;
    private static GraphQuery graphQuery;
    private static Transaction txn;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        testConfig = new NeighboursTestConfig();
        componentContainer = new ComponentsBuilder().create(testConfig);
        componentContainer.initialise();

        GraphDatabase database = componentContainer.get(GraphDatabase.class);

        graphQuery = componentContainer.get(GraphQuery.class);
        stationRepository = componentContainer.get(StationRepository.class);
        StationLocationsRepository stationLocations = componentContainer.get(StationLocationsRepository.class);

        txn = database.beginTx();

        CreateNeighbours createNeighbours = new CreateNeighbours(database, new IncludeAllFilter(), graphQuery, stationRepository, stationLocations, testConfig);
        createNeighbours.buildWithNoCommit(txn);

        routeCalculator = new RouteCalculatorTestFacade(componentContainer.get(RouteCalculator.class),
                stationRepository, txn);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        txn.rollback();
        componentContainer.close();
    }

    @Test
    void shouldCreateNeighbourRelationships() {

        Node shudehillTramNode = graphQuery.getStationNode(txn, TramStations.of(TramStations.Shudehill));
        Iterable<Relationship> busOutbounds = shudehillTramNode.getRelationships(Direction.OUTGOING, TransportRelationshipTypes.BUS_NEIGHBOUR);
        assertTrue(seenNode(txn, BusStations.ShudehillInterchange, busOutbounds, Relationship::getEndNode));

        Iterable<Relationship> tramOutbounds = shudehillTramNode.getRelationships(Direction.OUTGOING, TransportRelationshipTypes.TRAM_NEIGHBOUR);
        assertTrue(seenNode(txn, TramStations.of(TramStations.Victoria), tramOutbounds,  Relationship::getEndNode));

        Iterable<Relationship> inbounds = shudehillTramNode.getRelationships(Direction.INCOMING, TransportRelationshipTypes.TRAM_NEIGHBOUR);
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
            assertEquals(TransportMode.Tram, stage.getMode());
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
            assertEquals(TransportMode.Tram, first.getMode());
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

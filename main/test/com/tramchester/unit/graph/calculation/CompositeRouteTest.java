package com.tramchester.unit.graph.calculation;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.DiagramCreator;
import com.tramchester.domain.Journey;
import com.tramchester.domain.places.CompositeStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.RouteCostCalculator;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.repository.CompositeStationRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.resources.LocationJourneyPlanner;
import com.tramchester.testSupport.LocationJourneyPlannerTestFacade;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramTransportDataForTestFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.TestEnv.nearAltrincham;
import static com.tramchester.testSupport.TestEnv.nearKnutsfordBusStation;
import static org.junit.jupiter.api.Assertions.*;

class CompositeRouteTest {

    private static ComponentContainer componentContainer;
    private static SimpleGraphConfig config;

    private TramTransportDataForTestFactory.TramTransportDataForTest transportData;
    private RouteCalculator calculator;

    private TramServiceDate queryDate;
    private Transaction txn;
    private CompositeStation startCompositeStation;
    private TramTime queryTime;
    private CompositeStation fourthStationComposite;
    private LocationJourneyPlannerTestFacade locationJourneyPlanner;

    @BeforeAll
    static void onceBeforeAllTestRuns() throws IOException {
        config = new SimpleGraphConfig("CompositeRouteTest.db");
        TestEnv.deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder().
                overrideProvider(TramTransportDataForTestFactory.class).
                create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void onceAfterAllTestsRun() throws IOException {
        componentContainer.close();
        TestEnv.deleteDBIfPresent(config);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        transportData = (TramTransportDataForTestFactory.TramTransportDataForTest) componentContainer.get(TransportData.class);
        GraphDatabase database = componentContainer.get(GraphDatabase.class);
        StationRepository stationRepository = componentContainer.get(StationRepository.class);
        calculator = componentContainer.get(RouteCalculator.class);

        queryDate = new TramServiceDate(LocalDate.of(2014,6,30));
        queryTime = TramTime.of(7, 57);

        CompositeStationRepository compositeStationRepository = componentContainer.get(CompositeStationRepository.class);
        startCompositeStation = compositeStationRepository.findByName("startStation");
        fourthStationComposite = compositeStationRepository.findByName("Station4");

        txn = database.beginTx();

        locationJourneyPlanner = new LocationJourneyPlannerTestFacade(componentContainer.get(LocationJourneyPlanner.class),
                stationRepository, txn);

    }

    @NotNull
    private JourneyRequest createJourneyRequest(TramTime queryTime, int maxChanges) {
        return new JourneyRequest(queryDate, queryTime, false, maxChanges, config.getMaxJourneyDuration(), 2);
    }

    @AfterEach
    void afterEachTestRuns()
    {
        txn.close();
    }

    @Test
    void shouldHaveFirstCompositeStation() {
        assertNotNull(startCompositeStation);
        Set<Station> grouped = startCompositeStation.getContained();
        assertEquals(2, grouped.size());
        assertTrue(grouped.contains(transportData.getFirst()));
        assertTrue(grouped.contains(transportData.getFirstDupName()));
    }

    @Test
    void shouldHaveFourthCompositeStation() {
        assertNotNull(fourthStationComposite);
        Set<Station> grouped = fourthStationComposite.getContained();
        assertEquals(2, grouped.size());
        assertTrue(grouped.contains(transportData.getFourthStation()));
        assertTrue(grouped.contains(transportData.getFourthStationDupName()));
    }

    @Test
    void shouldHaveJourneyFromComposite() {
        JourneyRequest journeyRequest = createJourneyRequest(queryTime, 0);

        final CompositeStation start = this.startCompositeStation;
        final Station destination = transportData.getInterchange();

        Set<Journey> journeys = calculator.calculateRoute(txn, start, destination, journeyRequest).
                collect(Collectors.toSet());
        Assertions.assertEquals(1, journeys.size());
        Journey journey = (Journey) journeys.toArray()[0];

        List<TransportStage<?,?>> stages = journey.getStages();
        assertEquals(1, stages.size());
        TransportStage<?,?> firstStage = stages.get(0);

        assertEquals(transportData.getFirst(), firstStage.getFirstStation());
        assertEquals(destination.getId(), firstStage.getLastStation().getId());
    }

    @Test
    void shouldHaveJourneyFromNearComposite() {
        JourneyRequest journeyRequest = createJourneyRequest(queryTime, 3);

        final Station destination = transportData.getInterchange();

        // nearAltrincham = station 1
        Set<Journey> journeys = locationJourneyPlanner.quickestRouteForLocation(nearAltrincham,
                destination, journeyRequest, 3);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldHaveJourneyToComposite() {
        JourneyRequest journeyRequest = createJourneyRequest(queryTime, 1);

        Set<Journey> journeys = calculator.calculateRoute(txn, transportData.getFirst(),
                fourthStationComposite, journeyRequest).collect(Collectors.toSet());
        assertTrue(journeys.size()>=1);
        journeys.forEach(journey-> assertEquals(2, journey.getStages().size()));
    }

    @Test
    void shouldHaveJourneyToNearComposite() {
        JourneyRequest journeyRequest = createJourneyRequest(queryTime, 4);

        // nearKnutsfordBusStation = station 4
        Set<Journey> journeys = locationJourneyPlanner.quickestRouteForLocation(transportData.getFirst(),
                nearKnutsfordBusStation, journeyRequest, 3);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldHaveRouteCosts() {
        RouteCostCalculator routeCostCalculator = componentContainer.get(RouteCostCalculator.class);
        assertEquals(43, routeCostCalculator.getApproxCostBetween(txn, startCompositeStation, transportData.getLast()));
        assertEquals(43, routeCostCalculator.getApproxCostBetween(txn, transportData.getFirst(), transportData.getLast()));

        assertEquals(0, routeCostCalculator.getApproxCostBetween(txn, transportData.getFirst(), startCompositeStation));
        assertEquals(0, routeCostCalculator.getApproxCostBetween(txn, startCompositeStation, transportData.getFirst()));
    }

    @Disabled("for wip checkin")
    @Test
    void shouldHaveCorrectRouteHopsForComposites() {

        fail("todo - link composite route stations");
    }

    @Test
    void createDiagramOfTestNetwork() {
        DiagramCreator creator = componentContainer.get(DiagramCreator.class);
        Assertions.assertAll(() -> creator.create(Path.of("composite_test_network.dot"), transportData.getFirst(),
                Integer.MAX_VALUE, false));
    }

}

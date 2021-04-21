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
import com.tramchester.repository.TransportData;
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

import static com.tramchester.testSupport.reference.TramTransportDataForTestFactory.TramTransportDataForTest.FIRST_STATION;
import static com.tramchester.testSupport.reference.TramTransportDataForTestFactory.TramTransportDataForTest.INTERCHANGE;
import static org.junit.jupiter.api.Assertions.*;

class CompositeRouteTest {

    private static ComponentContainer componentContainer;
    private static SimpleGraphConfig config;

    private TramTransportDataForTestFactory.TramTransportDataForTest transportData;
    private RouteCalculator calculator;

    private TramServiceDate queryDate;
    private Transaction txn;
    private CompositeStation compositeStation;
    private TramTime queryTime;

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
        calculator = componentContainer.get(RouteCalculator.class);

        queryDate = new TramServiceDate(LocalDate.of(2014,6,30));
        queryTime = TramTime.of(7, 57);

        CompositeStationRepository compositeStationRepository = componentContainer.get(CompositeStationRepository.class);
        compositeStation = compositeStationRepository.findByName("startStation");

        txn = database.beginTx();
    }

    @NotNull
    private JourneyRequest createJourneyRequest(TramTime queryTime, int maxChanges) {
        return new JourneyRequest(queryDate, queryTime, false, maxChanges, config.getMaxJourneyDuration());
    }

    @AfterEach
    void afterEachTestRuns()
    {
        txn.close();
    }

    @Test
    void shouldHaveCompositeStation() {
        assertNotNull(compositeStation);
        Set<Station> grouped = compositeStation.getContained();
        assertEquals(2, grouped.size());
        assertTrue(grouped.contains(transportData.getFirst()));
        assertTrue(grouped.contains(transportData.getFirstDupName()));
    }

    @Test
    void shouldTestSimpleJourneyIsPossibleFromComposite() {
        JourneyRequest journeyRequest = createJourneyRequest(queryTime, 0);
        //journeyRequest.setDiag(true);

        Set<Journey> journeys = calculator.calculateRoute(txn, compositeStation,
                transportData.getInterchange(), journeyRequest).collect(Collectors.toSet());
        Assertions.assertEquals(1, journeys.size());

        assertFirstAndLast(journeys, FIRST_STATION, INTERCHANGE, 1, queryTime);
        journeys.forEach(journey-> Assertions.assertEquals(2, journey.getStages().size()));
    }

    @Test
    void shouldTestSimpleJourneyIsPossibleToInterchange() {
        JourneyRequest journeyRequest = createJourneyRequest(queryTime, 0);

        Set<Journey> journeys = calculator.calculateRoute(txn, transportData.getFirst(),
                transportData.getInterchange(), journeyRequest).collect(Collectors.toSet());
        Assertions.assertEquals(1, journeys.size());

        journeys.forEach(journey-> Assertions.assertEquals(1, journey.getStages().size()));
    }

    @Test
    void shouldHaveRouteCosts() {
        RouteCostCalculator routeCostCalculator = componentContainer.get(RouteCostCalculator.class);
        assertEquals(43, routeCostCalculator.getApproxCostBetween(txn, compositeStation, transportData.getLast()));
        assertEquals(43, routeCostCalculator.getApproxCostBetween(txn, transportData.getFirst(), transportData.getLast()));

        assertEquals(0, routeCostCalculator.getApproxCostBetween(txn, transportData.getFirst(), compositeStation));
        assertEquals(0, routeCostCalculator.getApproxCostBetween(txn, compositeStation, transportData.getFirst()));
    }

    @Test
    void createDiagramOfTestNetwork() {
        DiagramCreator creator = componentContainer.get(DiagramCreator.class);
        Assertions.assertAll(() -> creator.create(Path.of("composite_test_network.dot"),
                transportData.getFirst(), 100, false));
    }

    private void assertFirstAndLast(Set<Journey> journeys, String firstNonComposite, String destination,
                                          int passedStops, TramTime queryTime) {
        Journey journey = (Journey)journeys.toArray()[0];
        List<TransportStage<?,?>> stages = journey.getStages();
        TransportStage<?,?> connectingStage = stages.get(0);
        assertEquals(compositeStation, connectingStage.getFirstStation());
        assertEquals(firstNonComposite, connectingStage.getLastStation().forDTO());

        TransportStage<?,?> vehicleStage = stages.get(1);
        assertEquals(firstNonComposite, vehicleStage.getFirstStation().forDTO());
        assertEquals(destination, vehicleStage.getLastStation().forDTO());
        assertEquals(passedStops,  vehicleStage.getPassedStopsCount());
        assertTrue(vehicleStage.hasBoardingPlatform());

        TramTime departTime = vehicleStage.getFirstDepartureTime();
        assertTrue(departTime.isAfter(queryTime));

        assertTrue(vehicleStage.getDuration()>0);
    }

}

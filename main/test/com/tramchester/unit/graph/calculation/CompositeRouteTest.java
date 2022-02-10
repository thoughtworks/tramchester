package com.tramchester.unit.graph.calculation;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.DiagramCreator;
import com.tramchester.domain.Journey;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.InvalidDurationException;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.RouteCostCalculator;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.repository.RunningRoutesAndServices;
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
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static com.tramchester.testSupport.TestEnv.assertMinutesEquals;
import static com.tramchester.testSupport.reference.KnownLocations.nearAltrincham;
import static com.tramchester.testSupport.reference.KnownLocations.nearKnutsfordBusStation;
import static org.junit.jupiter.api.Assertions.*;

@Disabled("Need way to inject naptan test data here")
class CompositeRouteTest {

    private static ComponentContainer componentContainer;
    private static SimpleGraphConfig config;

    private TramTransportDataForTestFactory.TramTransportDataForTest transportData;

    private TramServiceDate queryDate;
    private Transaction txn;
    private StationGroup startGroup;
    private TramTime queryTime;
    private StationGroup fourthStationComposite;
    private LocationJourneyPlannerTestFacade locationJourneyPlanner;
    private RouteCalculatorTestFacade calculator;

    @BeforeAll
    static void onceBeforeAllTestRuns() throws IOException {
        config = new SimpleCompositeGraphConfig("CompositeRouteTest.db");
        TestEnv.deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder().
                overrideProvider(TramTransportDataForTestFactory.class).
                create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void onceAfterAllTestsRun() throws IOException {
        TestEnv.clearDataCache(componentContainer);
        componentContainer.close();
        TestEnv.deleteDBIfPresent(config);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        transportData = (TramTransportDataForTestFactory.TramTransportDataForTest) componentContainer.get(TransportData.class);
        GraphDatabase database = componentContainer.get(GraphDatabase.class);
        StationRepository stationRepository = componentContainer.get(StationRepository.class);
        RouteCalculator routeCalculator = componentContainer.get(RouteCalculator.class);

        queryDate = new TramServiceDate(LocalDate.of(2014,6,30));
        queryTime = TramTime.of(7, 57);

        StationGroupsRepository compositeStationRepository = componentContainer.get(StationGroupsRepository.class);

        // using fallback areas names as Naptan data is not loaded for this configuration
        // TODO update module injection code to allow naptan test data to be injected?
        startGroup = compositeStationRepository.findByName("Id{'area1'}");
        fourthStationComposite = compositeStationRepository.findByName("Id{'area4'}");

        txn = database.beginTx();

        locationJourneyPlanner = new LocationJourneyPlannerTestFacade(componentContainer.get(LocationJourneyPlanner.class),
                stationRepository, txn);

        calculator = new RouteCalculatorTestFacade(routeCalculator, stationRepository, txn);

    }

    @NotNull
    private JourneyRequest createJourneyRequest(TramTime queryTime, int maxChanges) {
        return new JourneyRequest(queryDate, queryTime, false, maxChanges,
                Duration.ofMinutes(config.getMaxJourneyDuration()), 2);
    }

    @AfterEach
    void afterEachTestRuns()
    {
        txn.close();
    }

    @Test
    void shouldCheckServiceIsRunning() {
        RunningRoutesAndServices runningRoutesAndServices = componentContainer.get(RunningRoutesAndServices.class);
        Service svcA = transportData.getServiceById(StringIdFor.createId("serviceAId"));
        RunningRoutesAndServices.FilterForDate running = runningRoutesAndServices.getFor(queryDate.getDate());
        assertTrue(running.isServiceRunningByTime(svcA.getId(), queryTime, 10), svcA.toString());
    }

    @Test
    void shouldHaveFirstCompositeStation() {
        assertNotNull(startGroup);
        Set<Station> grouped = startGroup.getContained();
        assertEquals(3, grouped.size());
        assertTrue(grouped.contains(transportData.getFirst()));
        assertTrue(grouped.contains(transportData.getFirstDupName()));
        assertTrue(grouped.contains(transportData.getFirstDup2Name()));
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

        final StationGroup start = startGroup;
        final Station destination = transportData.getInterchange();

//        Set<Journey> journeys = calculator.calculateRoute(txn, start, destination, journeyRequest).collect(Collectors.toSet());
        Set<Journey> journeys =  calculator.calculateRouteAsSet(start, destination, journeyRequest);

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
        Set<Journey> journeys = locationJourneyPlanner.quickestRouteForLocation(nearAltrincham.location(), destination, journeyRequest, 3);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldHaveJourneyToComposite() {
        JourneyRequest journeyRequest = createJourneyRequest(queryTime, 1);

//        Set<Journey> journeys = calculator.calculateRoute(txn, transportData.getFirst(),
//                fourthStationComposite, journeyRequest).collect(Collectors.toSet());

        Set<Journey> journeys = calculator.calculateRouteAsSet(transportData.getFirst(), fourthStationComposite, journeyRequest);

        assertTrue(journeys.size()>=1);
        journeys.forEach(journey-> assertEquals(2, journey.getStages().size()));
    }

    @Test
    void shouldHaveJourneyToNearComposite() {
        JourneyRequest journeyRequest = createJourneyRequest(queryTime, 4);

        // nearKnutsfordBusStation = station 4
        Set<Journey> journeys = locationJourneyPlanner.quickestRouteForLocation(transportData.getFirst(), nearKnutsfordBusStation.location(),
                journeyRequest, 3);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldFindTheCompositeStations() {
        assertNotNull(startGroup);
        assertNotNull(fourthStationComposite);
    }

    @Test
    void shouldHaveRouteCosts() throws InvalidDurationException {
        RouteCostCalculator routeCostCalculator = componentContainer.get(RouteCostCalculator.class);
        assertMinutesEquals(41, routeCostCalculator.getAverageCostBetween(txn, startGroup, transportData.getLast(), queryDate));
        assertMinutesEquals(41, routeCostCalculator.getAverageCostBetween(txn, transportData.getFirst(),
                transportData.getLast(), queryDate));

        assertMinutesEquals(0, routeCostCalculator.getAverageCostBetween(txn, transportData.getFirst(), startGroup, queryDate));
        assertMinutesEquals(0, routeCostCalculator.getAverageCostBetween(txn, startGroup, transportData.getFirst(), queryDate));
    }

    @Test
    void createDiagramOfTestNetwork() {
        DiagramCreator creator = componentContainer.get(DiagramCreator.class);
        Assertions.assertAll(() -> creator.create(Path.of("composite_test_network.dot"), transportData.getFirst(),
                Integer.MAX_VALUE, false));
    }

}

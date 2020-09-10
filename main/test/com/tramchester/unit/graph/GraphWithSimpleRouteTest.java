package com.tramchester.unit.graph;

import com.tramchester.Dependencies;
import com.tramchester.DiagramCreator;
import com.tramchester.config.DataSourceConfig;
import com.tramchester.domain.GTFSTransportationType;
import com.tramchester.domain.Journey;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.StationLocations;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.IntegrationTestConfig;
import com.tramchester.integration.TFGMTestDataSourceConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.resources.LocationJourneyPlanner;
import com.tramchester.testSupport.LocationJourneyPlannerTestFacade;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TransportDataForTestFactory;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.TransportDataForTestFactory.TestTransportData.*;

class GraphWithSimpleRouteTest {

    private static final String TMP_DB = "tmp.db";

    private static TransportDataForTestFactory.TestTransportData transportData;
    private static RouteCalculator calculator;
    private static Dependencies dependencies;
    private static GraphDatabase database;
    private static LocationJourneyPlannerTestFacade locationJourneyPlanner;
    private static SimpleGraphConfig config;

    private TramServiceDate queryDate;
    private TramTime queryTime;
    private Transaction txn;

    @BeforeAll
    static void onceBeforeAllTestRuns() throws IOException {
        dependencies = new Dependencies();

        StationLocations stationLocations = dependencies.get(StationLocations.class);

        TransportDataForTestFactory factory = new TransportDataForTestFactory(stationLocations);
        transportData = factory.get();

        config = new SimpleGraphConfig();
        FileUtils.deleteDirectory(config.getDBPath().toFile());

        dependencies.initialise(config, transportData);

        database = dependencies.get(GraphDatabase.class);
        calculator = dependencies.get(RouteCalculator.class);
    }

    @AfterAll
    static void onceAfterAllTestsRun() throws IOException {
        dependencies.close();
        FileUtils.deleteDirectory(config.getDBPath().toFile());
    }

    @BeforeEach
    void beforeEachTestRuns() {
        txn = database.beginTx();

        queryDate = new TramServiceDate(LocalDate.of(2014,6,30));
        queryTime = TramTime.of(7, 57);
        StationRepository stationRepo = dependencies.get(StationRepository.class);
        locationJourneyPlanner = new LocationJourneyPlannerTestFacade(dependencies.get(LocationJourneyPlanner.class), stationRepo, txn);
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
    void shouldTestSimpleJourneyIsPossible() {
        JourneyRequest journeyRequest = createJourneyRequest(queryTime, 0);
        Set<Journey> journeys = calculator.calculateRoute(txn, transportData.getFirst(),
                transportData.getSecond(), journeyRequest).
                collect(Collectors.toSet());
        Assertions.assertEquals(1, journeys.size());
        assertFirstAndLast(journeys, FIRST_STATION, SECOND_STATION, 0);
    }

    @Test
    void shouldHaveJourneyWithLocationBasedStart() {
        LatLong origin = TestEnv.nearAltrincham;

        Set<Journey> journeys = locationJourneyPlanner.quickestRouteForLocation(origin,  transportData.getSecond(),
                createJourneyRequest(TramTime.of(7,55), 0));

        Assertions.assertEquals(1, journeys.size());
        journeys.forEach(journey ->{
            List<TransportStage<?,?>> stages = journey.getStages();
            Assertions.assertEquals(2, stages.size());
            Assertions.assertEquals(stages.get(0).getMode(), TransportMode.Walk);
        });
    }

    @Test
    void shouldHaveJourneyWithLocationBasedEnd() {
        LatLong origin = TestEnv.nearShudehill;

        Set<Journey> journeys = locationJourneyPlanner.quickestRouteForLocation(transportData.getSecond(), origin,
                createJourneyRequest(TramTime.of(7,55), 0));

        Assertions.assertEquals(1, journeys.size());
        journeys.forEach(journey ->{
            List<TransportStage<?,?>> stages = journey.getStages();
            Assertions.assertEquals(1, stages.size());
            Assertions.assertEquals(stages.get(0).getMode(), TransportMode.Walk);
        });
    }

    @Test
    void shouldTestSimpleJourneyIsPossibleToInterchange() {
        JourneyRequest journeyRequest = createJourneyRequest(queryTime, 0);

        Set<Journey> journeys = calculator.calculateRoute(txn, transportData.getFirst(),
                transportData.getInterchange(), journeyRequest).collect(Collectors.toSet());
        Assertions.assertEquals(1, journeys.size());
        assertFirstAndLast(journeys, FIRST_STATION, INTERCHANGE, 1);
        checkForPlatforms(journeys);
        journeys.forEach(journey-> Assertions.assertEquals(1, journey.getStages().size()));
    }

    private void checkForPlatforms(Set<Journey> journeys) {
        journeys.forEach(journey -> journey.getStages().forEach(stage -> Assertions.assertTrue(stage.hasBoardingPlatform())));
    }

    @Test
    void shouldTestSimpleJourneyIsNotPossible() {
        Set<Journey> journeys = calculator.calculateRoute(txn, transportData.getFirst(),
                transportData.getInterchange(), createJourneyRequest(TramTime.of(9, 0), 3)).collect(Collectors.toSet());
        Assertions.assertEquals(0, journeys.size());
    }

    @Test
    void shouldTestJourneyEndOverWaitLimitIsPossible() {
        JourneyRequest journeyRequest = createJourneyRequest(queryTime, 0);

        Set<Journey> journeys = calculator.calculateRoute(txn, transportData.getFirst(),
                transportData.getLast(), journeyRequest).collect(Collectors.toSet());
        Assertions.assertEquals(1, journeys.size());
        assertFirstAndLast(journeys, FIRST_STATION, LAST_STATION, 2);
        journeys.forEach(journey-> Assertions.assertEquals(1, journey.getStages().size()));
    }

    @Test
    void shouldTestNoJourneySecondToStart() {
        JourneyRequest journeyRequest = createJourneyRequest(queryTime, 0);

        Set<Journey> journeys = calculator.calculateRoute(txn, transportData.getSecond(),
                transportData.getFirst(), journeyRequest).collect(Collectors.toSet());
        Assertions.assertEquals(0,journeys.size());
    }

    @Test
    void shouldTestJourneyInterchangeToFive() {
        JourneyRequest journeyRequest = createJourneyRequest(queryTime, 0);

        Set<Journey> journeys = calculator.calculateRoute(txn, transportData.getInterchange(),
                transportData.getFifthStation(), journeyRequest).collect(Collectors.toSet());
        Assertions.assertFalse(journeys.size()>=1);

        JourneyRequest journeyRequestB = createJourneyRequest(TramTime.of(8, 10), 3);
        journeys = calculator.calculateRoute(txn, transportData.getInterchange(),
                transportData.getFifthStation(), journeyRequestB).collect(Collectors.toSet());
        Assertions.assertTrue(journeys.size()>=1);
        journeys.forEach(journey-> Assertions.assertEquals(1, journey.getStages().size()));
    }

    @Test
    void shouldTestJourneyEndOverWaitLimitViaInterchangeIsPossible() {
        JourneyRequest journeyRequest = createJourneyRequest(queryTime, 1);

        Set<Journey> journeys = calculator.calculateRoute(txn, transportData.getFirst(),
                transportData.getFourthStation(), journeyRequest).collect(Collectors.toSet());
       Assertions.assertTrue(journeys.size()>=1);
       checkForPlatforms(journeys);
        journeys.forEach(journey-> Assertions.assertEquals(2, journey.getStages().size()));
    }

    @Test
    void shouldTestJourneyAnotherWaitLimitViaInterchangeIsPossible() {
        JourneyRequest journeyRequest = createJourneyRequest(queryTime, 1);

        Set<Journey> journeys = calculator.calculateRoute(txn, transportData.getFirst(),
                transportData.getFifthStation(), journeyRequest).collect(Collectors.toSet());
        Assertions.assertTrue(journeys.size()>=1);
        checkForPlatforms(journeys);
        journeys.forEach(journey-> Assertions.assertEquals(2, journey.getStages().size()));
    }

    @Test
    void createDiagramOfTestNetwork() {
        DiagramCreator creator = new DiagramCreator(database, Integer.MAX_VALUE);
        Assertions.assertAll(() -> creator.create("test_network.dot", transportData.getFirst()));
    }

    private void assertFirstAndLast(Set<Journey> journeys, String firstStation, String secondStation,
                                    int passedStops) {
        Journey journey = (Journey)journeys.toArray()[0];
        List<TransportStage<?,?>> stages = journey.getStages();
        TransportStage<?,?> vehicleStage = stages.get(0);
        Assertions.assertEquals(firstStation, vehicleStage.getFirstStation().forDTO());
        Assertions.assertEquals(secondStation, vehicleStage.getLastStation().forDTO());
        Assertions.assertEquals(passedStops,  vehicleStage.getPassedStops());
        Assertions.assertTrue(vehicleStage.hasBoardingPlatform());

        TramTime departTime = vehicleStage.getFirstDepartureTime();
        Assertions.assertTrue(departTime.isAfter(queryTime));

        Assertions.assertTrue(vehicleStage.getDuration()>0);
    }

    private static class SimpleGraphConfig extends IntegrationTestConfig {

        public SimpleGraphConfig() {
            super("unitTest", TMP_DB);
        }

        @Override
        protected List<DataSourceConfig> getDataSourceFORTESTING() {
            TFGMTestDataSourceConfig tfgmTestDataSourceConfig = new TFGMTestDataSourceConfig("data/tram",
                    Collections.singleton(GTFSTransportationType.tram));
            return Collections.singletonList(tfgmTestDataSourceConfig);
        }

        @Override
        public int getNumberQueries() { return 1; }

        @Override
        public int getQueryInterval() {
            return 6;
        }
    }
}

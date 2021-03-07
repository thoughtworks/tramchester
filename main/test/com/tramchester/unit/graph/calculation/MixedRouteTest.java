package com.tramchester.unit.graph.calculation;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.DiagramCreator;
import com.tramchester.config.DataSourceConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.testSupport.GraphDBTestConfig;
import com.tramchester.integration.testSupport.IntegrationTestConfig;
import com.tramchester.integration.testSupport.TFGMTestDataSourceConfig;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.MixedTransportDataProvider;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.reference.MixedTransportDataProvider.TestMixedTransportData.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MixedRouteTest {

    private static MixedTransportDataProvider.TestMixedTransportData transportData;
    private static RouteCalculator calculator;
    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static MixedRouteTest.SimpleGraphConfig config;

    private TramServiceDate queryDate;
    private TramTime queryTime;
    private Transaction txn;

    @BeforeAll
    static void onceBeforeAllTestRuns() throws IOException {
        config = new MixedRouteTest.SimpleGraphConfig();

        FileUtils.deleteDirectory(config.getDBPath().toFile());

        componentContainer = new ComponentsBuilder<MixedTransportDataProvider>().
                overrideProvider(MixedTransportDataProvider.class).
                create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initContainer();

        transportData = (MixedTransportDataProvider.TestMixedTransportData) componentContainer.get(TransportData.class);
        database = componentContainer.get(GraphDatabase.class);
        calculator = componentContainer.get(RouteCalculator.class);
    }

    @AfterAll
    static void onceAfterAllTestsRun() throws IOException {
        componentContainer.close();
        FileUtils.deleteDirectory(config.getDBPath().toFile());
    }

    @BeforeEach
    void beforeEachTestRuns() {
        txn = database.beginTx();

        queryDate = new TramServiceDate(LocalDate.of(2014,6,30));
        queryTime = TramTime.of(7, 57);
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
        assertEquals(1, journeys.size());
        assertFirstAndLastForOneStage(journeys, FIRST_STATION, SECOND_STATION, 0, queryTime);
    }

    @Test
    void shouldTestMultiStopJourneyIsPossible() {
        JourneyRequest journeyRequest = createJourneyRequest(queryTime, 0);
        Set<Journey> journeys = calculator.calculateRoute(txn, transportData.getFirst(),
                transportData.getLast(), journeyRequest).
                collect(Collectors.toSet());
        assertEquals(1, journeys.size());
        assertFirstAndLastForOneStage(journeys, FIRST_STATION, LAST_STATION, 2, queryTime);
    }

    @Test
    void shouldTestMultiStopJourneyFerryIsPossible() {
        ///
        // Note relies on multi-mode stations automatically beign seen as interchanges
        // Change at Interchange ONLY is enable in config below
        ///
        assertTrue(config.getChangeAtInterchangeOnly(),"valid precondition");
        JourneyRequest journeyRequest = createJourneyRequest(queryTime, 1);

        Set<Journey> journeys = calculator.calculateRoute(txn, transportData.getFirst(),
                transportData.getFourthStation(), journeyRequest).collect(Collectors.toSet());
        assertEquals(1, journeys.size(), "journeys");
        Journey journey = (Journey) journeys.toArray()[0];

        List<TransportStage<?,?>> stages = journey.getStages();
        assertEquals(2, stages.size());

        TransportStage<?,?> first = stages.get(0);
        assertEquals(FIRST_STATION, first.getFirstStation().forDTO());

        TransportStage<?,?> last = stages.get(1);
        assertEquals(STATION_FOUR, last.getLastStation().forDTO());

    }

    @Test
    void createDiagramOfTestNetwork() {
        DiagramCreator creator = componentContainer.get(DiagramCreator.class);
        //DiagramCreator creator = new DiagramCreator(database, ready);
        Assertions.assertAll(() -> creator.create("mixed_test_network.dot", transportData.getFirst(), Integer.MAX_VALUE));
    }

    private static void assertFirstAndLastForOneStage(Set<Journey> journeys, String firstStation, String secondStation,
                                                      int passedStops, TramTime queryTime) {
        Journey journey = (Journey)journeys.toArray()[0];
        List<TransportStage<?,?>> stages = journey.getStages();
        TransportStage<?,?> vehicleStage = stages.get(0);
        assertEquals(firstStation, vehicleStage.getFirstStation().forDTO());
        assertEquals(secondStation, vehicleStage.getLastStation().forDTO());
        assertEquals(passedStops,  vehicleStage.getPassedStopsCount());
        Assertions.assertFalse(vehicleStage.hasBoardingPlatform());

        TramTime departTime = vehicleStage.getFirstDepartureTime();
        assertTrue(departTime.isAfter(queryTime));

        assertTrue(vehicleStage.getDuration()>0);
    }

    private static class SimpleGraphConfig extends IntegrationTestConfig {

        public SimpleGraphConfig() {
            super(new GraphDBTestConfig("unitTest", "MixedRouteTest.db"));
        }

        @Override
        public boolean getChangeAtInterchangeOnly() {
            return true;
        }

        @Override
        protected List<DataSourceConfig> getDataSourceFORTESTING() {
            Set<GTFSTransportationType> modes = new HashSet<>(
                    Arrays.asList(GTFSTransportationType.bus, GTFSTransportationType.tram, GTFSTransportationType.ferry));

            TFGMTestDataSourceConfig tfgmTestDataSourceConfig = new TFGMTestDataSourceConfig("unused",
                    modes, Collections.singleton(TransportMode.Tram));
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

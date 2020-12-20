package com.tramchester.unit.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.DataSourceConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.testSupport.IntegrationTestConfig;
import com.tramchester.integration.testSupport.TFGMTestDataSourceConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.MixedTransportDataForTestProvider;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.reference.MixedTransportDataForTestProvider.TestMixedTransportData.FIRST_STATION;
import static com.tramchester.testSupport.reference.MixedTransportDataForTestProvider.TestMixedTransportData.SECOND_STATION;

class GraphWithSimpleMixedRouteTest {
    private static final String TMP_DB = "tmp2.db";

    private static MixedTransportDataForTestProvider.TestMixedTransportData transportData;
    private static RouteCalculator calculator;
    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static GraphWithSimpleMixedRouteTest.SimpleGraphConfig config;

    private TramServiceDate queryDate;
    private TramTime queryTime;
    private Transaction txn;

    @BeforeAll
    static void onceBeforeAllTestRuns() throws IOException {
        config = new GraphWithSimpleMixedRouteTest.SimpleGraphConfig();

        FileUtils.deleteDirectory(config.getDBPath().toFile());

        componentContainer = new ComponentsBuilder<MixedTransportDataForTestProvider>().
                overrideProvider(MixedTransportDataForTestProvider.class).
                create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initContainer();

        transportData = (MixedTransportDataForTestProvider.TestMixedTransportData) componentContainer.get(TransportData.class);
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
        StationRepository stationRepo = componentContainer.get(StationRepository.class);
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
        assertFirstAndLast(journeys, FIRST_STATION, SECOND_STATION, 0, queryTime);
    }

    private static void assertFirstAndLast(Set<Journey> journeys, String firstStation, String secondStation,
                                   int passedStops, TramTime queryTime) {
        Journey journey = (Journey)journeys.toArray()[0];
        List<TransportStage<?,?>> stages = journey.getStages();
        TransportStage<?,?> vehicleStage = stages.get(0);
        Assertions.assertEquals(firstStation, vehicleStage.getFirstStation().forDTO());
        Assertions.assertEquals(secondStation, vehicleStage.getLastStation().forDTO());
        Assertions.assertEquals(passedStops,  vehicleStage.getPassedStops());
        Assertions.assertFalse(vehicleStage.hasBoardingPlatform());

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
            Set<GTFSTransportationType> modes = new HashSet<>(Arrays.asList(GTFSTransportationType.bus, GTFSTransportationType.tram));
            TFGMTestDataSourceConfig tfgmTestDataSourceConfig = new TFGMTestDataSourceConfig("unused",
                    modes);
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

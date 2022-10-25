package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.JourneysForBox;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.dates.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.BoundingBoxWithStations;
import com.tramchester.geo.StationLocations;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.RouteCalculatorForBoxes;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.PiccGardens2022;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class RouteCalculatorForBoundingBoxTest {
    // Note this needs to be > time for whole test fixture, see note below in @After
    private static final int TXN_TIMEOUT = 5*60;

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static TramchesterConfig testConfig;

    private RouteCalculatorForBoxes calculator;
    private final TramDate when = TestEnv.testDay();
    private Transaction txn;
    private StationLocations stationLocations;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        testConfig = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        database = componentContainer.get(GraphDatabase.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        txn = database.beginTx(TXN_TIMEOUT, TimeUnit.SECONDS);
        calculator = componentContainer.get(RouteCalculatorForBoxes.class);
        stationLocations = componentContainer.get(StationLocations.class);
        stationRepository = componentContainer.get(StationRepository.class);
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldFindJourneysForBoundedBoxStations() {
        BoundingBox bounds = stationLocations.getBounds();
        long gridSize = (bounds.getMaxNorthings()-bounds.getMinNorthings()) / 100;

        List<BoundingBoxWithStations> grouped = stationLocations.getStationsInGrids(gridSize).collect(Collectors.toList());

        long maxNumberOfJourneys = 3;
        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(when), TramTime.of(9,30),
                false, 3, Duration.ofMinutes(testConfig.getMaxJourneyDuration()), maxNumberOfJourneys,
                Collections.emptySet());

        LocationSet destinations = LocationSet.singleton(TramStations.StPetersSquare.from(stationRepository));

        Stream<JourneysForBox> stream = calculator.calculateRoutes(destinations, journeyRequest, grouped);
        List<JourneysForBox> groupedJourneys = stream.collect(Collectors.toList());

        assertFalse(groupedJourneys.isEmpty());

        List<JourneysForBox> missed = groupedJourneys.stream().filter(group -> group.getJourneys().isEmpty()).collect(Collectors.toList());

        assertEquals(1, missed.size(), missed.toString()); // when start and dest match

        groupedJourneys.forEach(group -> group.getJourneys().forEach(journey -> {
            assertTrue(journey.getStages().size()>0); // catch case where starting point is dest
        } ));
    }
}

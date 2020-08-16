package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.domain.JourneysForBox;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.BoundingBoxWithStations;
import com.tramchester.geo.StationLocations;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class RouteCalulcatorForBoundingBoxTest {
    // Note this needs to be > time for whole test fixture, see note below in @After
    private static final int TXN_TIMEOUT = 5*60;

    private static Dependencies dependencies;
    private static GraphDatabase database;
    private static IntegrationTramTestConfig testConfig;

    private RouteCalculator calculator;
    private final LocalDate when = TestEnv.testDay();
    private Transaction txn;
    private StationLocations stationLocations;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        dependencies = new Dependencies();
        testConfig = new IntegrationTramTestConfig();
        dependencies.initialise(testConfig);
        database = dependencies.get(GraphDatabase.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        txn = database.beginTx(TXN_TIMEOUT, TimeUnit.SECONDS);
        calculator = dependencies.get(RouteCalculator.class);
        stationLocations = dependencies.get(StationLocations.class);
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldFindJourneysForBoundedBoxStations() {
        BoundingBox bounds = stationLocations.getBounds();
        long gridSize = (bounds.getMaxNorthings()-bounds.getMinNorthings()) / 100;

        List<BoundingBoxWithStations> grouped = stationLocations.getGroupedStations(gridSize).collect(Collectors.toList());

        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(when), TramTime.of(9,30),
                false, 3, testConfig.getMaxJourneyDuration());

        Set<Station> destinations = Collections.singleton(Stations.StPetersSquare);

        long numberToFind = 3;
        Stream<JourneysForBox> stream = calculator.calculateRoutes(destinations, journeyRequest, grouped, numberToFind);
        List<JourneysForBox> groupedJourneys = stream.collect(Collectors.toList());

        List<JourneysForBox> missed = groupedJourneys.stream().filter(group -> group.getJourneys().isEmpty()).collect(Collectors.toList());
        assertEquals(1, missed.size(), missed.toString()); // when start and dest match

        groupedJourneys.forEach(group -> group.getJourneys().forEach(journey -> {
            assertTrue(journey.getStages().size()>0); // catch case where starting point is dest
        } ));
    }
}

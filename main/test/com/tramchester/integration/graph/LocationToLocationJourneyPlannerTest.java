package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.TestConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.integration.Stations;
import com.tramchester.resources.LocationToLocationJourneyPlanner;
import org.junit.*;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tramchester.TestConfig.nearAltrincham;
import static com.tramchester.TestConfig.nearPiccGardens;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class LocationToLocationJourneyPlannerTest {
    public static final int TXN_TIMEOUT = 5*60;

    private static Dependencies dependencies;
    private static TramchesterConfig testConfig;
    private static GraphDatabaseService database;

    private LocalDate nextTuesday = TestConfig.nextTuesday(0);
    private Transaction tx;
    private LocationToLocationJourneyPlanner planner;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        testConfig = new IntegrationTramTestConfig();
        dependencies.initialise(testConfig);
        database = dependencies.get(GraphDatabaseService.class);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Before
    public void beforeEachTestRuns() {
        tx = database.beginTx(TXN_TIMEOUT, TimeUnit.SECONDS);
        planner = dependencies.get(LocationToLocationJourneyPlanner.class);
    }

    @After
    public void afterEachTestRuns() {
        tx.close();
    }

    @Test
    public void shouldHaveDirectWalkNearPiccadily() {
        List<TramTime> times = Collections.singletonList(TramTime.of(9, 0));
        TramServiceDate queryDate = new TramServiceDate(nextTuesday);

        Set<Journey> unsortedResults = planner.quickestRouteForLocation(nearPiccGardens,
                Stations.PiccadillyGardens.getId(), times, queryDate).collect(Collectors.toSet());

        assertEquals(1, unsortedResults.size());
        unsortedResults.forEach(journey -> {
            List<TransportStage> stages = journey.getStages();
            WalkingStage first = (WalkingStage) stages.get(0);
            assertEquals(nearPiccGardens, first.getStart().getLatLong());
            assertEquals(Stations.PiccadillyGardens, first.getDestination());
        });
    }

    @Test
    public void shouldFindJourneyWithWalkingEarlyMorning() {
        Set<Journey> results = getJourneysForWalkAndTram(nearAltrincham, Stations.Deansgate.getId(),
                TramTime.of(8,00)); //, new StationWalk(Stations.Altrincham, 13));
        assertFalse(results.isEmpty());
        results.forEach(journey -> assertEquals(2, journey.getStages().size()));
    }

    @Test
    public void shouldFindJourneyWithWalkingEndOfDay() {
        Set<Journey> results = getJourneysForWalkAndTram(nearAltrincham, Stations.Deansgate.getId(),
                TramTime.of(23,00)); //, new StationWalk(Stations.Altrincham, 13));
        assertFalse(results.isEmpty());
        results.forEach(journey -> assertEquals(2, journey.getStages().size()));
    }

    @Test
    public void shouldFindWalkOnlyIfNearDestinationStationSingleStationWalk() {
        Set<Journey> results = getJourneysForWalkAndTram(nearPiccGardens, Stations.PiccadillyGardens.getId(),
                TramTime.of(9,00)); //, new StationWalk(Stations.PiccadillyGardens, 3));
        assertFalse(results.isEmpty());
        results.forEach(journey-> {
            assertEquals(1,journey.getStages().size());
            TransportStage rawStage = journey.getStages().get(0);
            assertEquals(TransportMode.Walk, rawStage.getMode());
            assertEquals(Stations.PiccadillyGardens, ((WalkingStage) rawStage).getDestination());
            assertEquals(nearPiccGardens, ((WalkingStage) rawStage).getStart().getLatLong());
            assertEquals(3, ((WalkingStage) rawStage).getDuration());
        });
    }

    private Set<Journey> getJourneysForWalkAndTram(LatLong latLong, String destinationId, TramTime queryTime) {
        List<TramTime> times = Arrays.asList(queryTime);
        TramServiceDate date = new TramServiceDate(nextTuesday);

        return planner.quickestRouteForLocation(latLong, destinationId, times, date).collect(Collectors.toSet());

    }
}

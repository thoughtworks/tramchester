package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.TestConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.RawJourney;
import com.tramchester.domain.RawStage;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.graph.*;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.integration.Stations;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

public class TramNetworkTraverserTest {
    private static Dependencies dependencies;
    private static TramchesterConfig testConfig;
    private static boolean edgePerTrip;

    private RouteCalculator calculator;
    private LocalDate nextTuesday = TestConfig.nextTuesday(0);

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        testConfig = new IntegrationTramTestConfig();
        edgePerTrip = testConfig.getEdgePerTrip();
        dependencies.initialise(testConfig);
    }

    @Before
    public void beforeEachTestRuns() {
        calculator = dependencies.get(RouteCalculator.class);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Test
    public void shouldNotGenerateDuplicateJourneys() {
        assumeTrue(edgePerTrip);

        Set<List<RawStage>> stages = new HashSet<>();

        List<LocalTime> queryTimes = new LinkedList<>();
        queryTimes.add(LocalTime.of(11,45));
        Set<RawJourney> journeys = calculator.calculateRoute(Stations.Bury.getId(), Stations.Altrincham.getId(), queryTimes,
                new TramServiceDate(nextTuesday), 100);
        assertTrue(journeys.size()>0);

        journeys.forEach(journey -> {
            assertFalse(stages.toString(), stages.contains(journey.getStages()));
            stages.add(journey.getStages());
            });

    }

}

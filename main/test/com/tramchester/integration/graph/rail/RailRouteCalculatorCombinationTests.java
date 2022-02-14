package com.tramchester.integration.graph.rail;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.integration.testSupport.RouteCalculationCombinations;
import com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.TrainTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.neo4j.graphdb.Transaction;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.tramchester.domain.reference.TransportMode.Train;

@TrainTest
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
@Disabled("currently take too long")
class RailRouteCalculatorCombinationTests {

    // TODO this needs to be > time for whole test fixture, see note below in @After
    private static final int TXN_TIMEOUT = 5*60;

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private RouteCalculationCombinations combinations;

    private final LocalDate when = TestEnv.testDay();
    private Transaction txn;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig config = new IntegrationRailTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
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
        combinations = new RouteCalculationCombinations(componentContainer);
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldHaveInterchangesToInterchanges() {
        TramTime travelTime = TramTime.of(8, 0);

        JourneyRequest request = new JourneyRequest(new TramServiceDate(when), travelTime, false,
                10, Duration.ofHours(8), 1, getRequestedModes());

        combinations.validateAllHaveAtLeastOneJourney(combinations.InterchangeToInterchange(Train), request);
    }

    private Set<TransportMode> getRequestedModes() {
        return Collections.emptySet();
    }

    @Test
    void shouldHaveEndsOfLinesToEndsOfLines() {
        TramTime travelTime = TramTime.of(8, 0);

        JourneyRequest request = new JourneyRequest(new TramServiceDate(when), travelTime, false,
                10, Duration.ofHours(8), 1, getRequestedModes());

        combinations.validateAllHaveAtLeastOneJourney(combinations.EndOfRoutesToEndOfRoutes(Train), request);
    }

}

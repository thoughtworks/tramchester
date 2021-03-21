package com.tramchester.integration.graph.trains;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.FindStationsByNumberLinks;
import com.tramchester.integration.testSupport.IntegrationTrainTestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TrainStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class FindStationsByNumberLinksTrainTest {
    private static ComponentContainer componentContainer;
    private FindStationsByNumberLinks discoverer;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig config = new IntegrationTrainTestConfig();

        componentContainer = new ComponentsBuilder<>().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        discoverer = componentContainer.get(FindStationsByNumberLinks.class);
    }

    @Test
    void shouldIdForkPointsFromTrainNetwork() {
        int threshhold = 3;
        IdSet<Station> found = discoverer.findAtLeastNConnectionsFrom(TransportMode.Train, threshhold);
        assertEquals(913, found.size());

        assertTrue(found.contains(TrainStations.ManchesterPiccadilly.getId()));
        assertTrue(found.contains(TrainStations.LondonEuston.getId()));
        assertTrue(found.contains(TrainStations.Stockport.getId()));

        assertFalse(found.contains(TrainStations.Mobberley.getId()));
        assertFalse(found.contains(TrainStations.Hale.getId()));
    }

}

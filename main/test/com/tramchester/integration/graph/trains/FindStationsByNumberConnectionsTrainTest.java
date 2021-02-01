package com.tramchester.integration.graph.trains;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.FindStationsByNumberConnections;
import com.tramchester.integration.testSupport.IntegrationTrainTestConfig;
import com.tramchester.integration.testSupport.IntegrationTramTestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TrainStations;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class FindStationsByNumberConnectionsTrainTest {
    private static ComponentContainer componentContainer;
    private FindStationsByNumberConnections discoverer;

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
        discoverer = componentContainer.get(FindStationsByNumberConnections.class);
    }

    @Test
    void shouldIdForkPointsFromTramNetwork() {
        int threshhold = 3;
        IdSet<Station> found = discoverer.findFor(TransportMode.Train, threshhold, false);
        assertEquals(1095, found.size());
        assertTrue(found.contains(TrainStations.ManchesterPiccadilly.getId()));
        assertTrue(found.contains(TrainStations.LondonEuston.getId()));
        assertTrue(found.contains(TrainStations.Stockport.getId()));

        assertFalse(found.contains(TrainStations.Mobberley.getId()));
        assertFalse(found.contains(TrainStations.Hale.getId()));
    }

    @Test
    void shouldFineEndsOfLines() {
        int threshhold = 1;
        IdSet<Station> found = discoverer.findFor(TransportMode.Train, threshhold, true);

//        IdSet<Station> expected = TramStations.EndOfTheLine.stream().map(TramStations::getId).collect(IdSet.idCollector());
//        assertEquals(expected, found);
    }

    @Test
    void shouldGetZeroMatchForBuses() {
        int threshhold = 1;
        IdSet<Station> found = discoverer.findFor(TransportMode.Bus, threshhold, false);
        assertEquals(0, found.size());
    }

}

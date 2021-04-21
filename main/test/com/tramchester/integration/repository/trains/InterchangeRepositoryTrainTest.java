package com.tramchester.integration.repository.trains;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.integration.testSupport.train.IntegrationTrainTestConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TrainStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;

class InterchangeRepositoryTrainTest {
    private static ComponentContainer componentContainer;
    private InterchangeRepository interchangeRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder().create(new IntegrationTrainTestConfig(), TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        interchangeRepository = componentContainer.get(InterchangeRepository.class);
    }

    @Test
    void shouldHaveExpectedInterchanges() {

        assertTrue(interchangeRepository.isInterchange(TrainStations.of(TrainStations.ManchesterPiccadilly)));
        assertTrue(interchangeRepository.isInterchange(TrainStations.of(TrainStations.Stockport)));
        assertTrue(interchangeRepository.isInterchange(TrainStations.of(TrainStations.LondonEuston)));

        assertFalse(interchangeRepository.isInterchange(TrainStations.of(TrainStations.Hale)));
        assertFalse(interchangeRepository.isInterchange(TrainStations.of(TrainStations.Knutsford)));
        assertFalse(interchangeRepository.isInterchange(TrainStations.of(TrainStations.Mobberley)));
    }


}

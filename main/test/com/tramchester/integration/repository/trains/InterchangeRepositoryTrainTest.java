package com.tramchester.integration.repository.trains;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.input.TramInterchanges;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.IntegrationTrainTestConfig;
import com.tramchester.integration.testSupport.IntegrationTramTestConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TrainStations;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class InterchangeRepositoryTrainTest {
    private static ComponentContainer componentContainer;
    private InterchangeRepository repository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder<>().create(new IntegrationTrainTestConfig(), TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        repository = componentContainer.get(InterchangeRepository.class);
    }

    @Test
    void shouldHaveExpectedInterchanges() {

        assertTrue(repository.isInterchange(TrainStations.of(TrainStations.ManchesterPiccadilly)));
        assertTrue(repository.isInterchange(TrainStations.of(TrainStations.Stockport)));
        assertTrue(repository.isInterchange(TrainStations.of(TrainStations.LondonEuston)));

        assertFalse(repository.isInterchange(TrainStations.of(TrainStations.Hale)));
        assertFalse(repository.isInterchange(TrainStations.of(TrainStations.Knutsford)));
        assertFalse(repository.isInterchange(TrainStations.of(TrainStations.Mobberley)));


    }

}

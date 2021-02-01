package com.tramchester.integration.repository;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.TramInterchanges;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.IntegrationTramTestConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.*;

class InterchangeRepositoryTramTest {
    private static ComponentContainer componentContainer;
    private InterchangeRepository repository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder<>().create(new IntegrationTramTestConfig(), TestEnv.NoopRegisterMetrics());
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
    void shouldHaveOfficialTramInterchanges() {
        for (IdFor<Station> interchange : TramInterchanges.stations()) {
            Assertions.assertTrue(repository.isInterchange(interchange), interchange.toString());
        }
    }

}

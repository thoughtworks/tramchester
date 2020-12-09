package com.tramchester.integration.repository;

import com.tramchester.ComponentContainer;
import com.tramchester.Dependencies;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.input.TramInterchanges;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.IntegrationTramTestConfig;
import com.tramchester.repository.InterchangeRepository;
import org.junit.jupiter.api.*;

class TramInterchangeRepositoryTest {
    private static ComponentContainer componentContainer;
    private InterchangeRepository repository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new Dependencies();
        componentContainer.initialise(new IntegrationTramTestConfig());
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
    void shouldFindTramInterchanges() {
        for (IdFor<Station> interchange : TramInterchanges.stations()) {
            Assertions.assertTrue(repository.isInterchange(interchange));
        }
    }

}

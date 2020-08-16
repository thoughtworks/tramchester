package com.tramchester.integration.repository;

import com.tramchester.Dependencies;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.input.TramInterchanges;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.InterchangeRepository;
import org.junit.jupiter.api.*;

import java.io.IOException;

class TramInterchangeRepositoryTest {
    private static Dependencies dependencies;
    private InterchangeRepository repository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTramTestConfig());
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        repository = dependencies.get(InterchangeRepository.class);
    }

    @Test
    void shouldFindTramInterchanges() {
        for (IdFor<Station> interchange : TramInterchanges.stations()) {
            Assertions.assertTrue(repository.isInterchange(interchange));
        }
    }

}

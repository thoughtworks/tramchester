package com.tramchester.integration.repository;

import com.tramchester.Dependencies;
import com.tramchester.domain.input.TramInterchanges;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.InterchangeRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

class TramInterchangeRepositoryTest {
    private static Dependencies dependencies;
    private InterchangeRepository repository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {
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
        for (String interchange : TramInterchanges.stations()) {
            Assertions.assertTrue(repository.isInterchange(interchange));
        }
    }

}

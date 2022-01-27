package com.tramchester.integration.repository.nptg;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.dataimport.nptg.NPTGData;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfigWithNaptan;
import com.tramchester.repository.nptg.NPTGRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.TrainTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TrainTest
public class NPTGRepositoryTest {
    private static GuiceContainerDependencies componentContainer;
    private NPTGRepository repository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfigWithNaptan();
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void onceAfterAllTestsHaveRun() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        repository = componentContainer.get(NPTGRepository.class);
    }

    @Test
    void shouldGetKnownLocationData() {
        final String code = "N0077434";

        assertTrue(repository.hasNptgCode(code));
        NPTGData result = repository.getByNptgCode(code);

        assertEquals("Ashley Heath", result.getLocalityName());
        assertEquals("083", result.getAdministrativeAreaCode());
        assertEquals("Altrincham", result.getQualifierName());
    }

    @Test
    void shouldFindExpectedCodes() {
        // repro issue with naptan data enrichment
        assertTrue(repository.hasNptgCode("E0027629"));
        assertTrue(repository.hasNptgCode("E0027768"));
    }
}

package com.tramchester.integration.dataimport.postcodes;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.id.CaseInsensitiveId;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.postcodes.PostcodeRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BusTest
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class PostcodeRepositoryBusTest {

    private static ComponentContainer componentContainer;
    private PostcodeRepository repository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig testConfig = new IntegrationBusTestConfig();
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        repository = componentContainer.get(PostcodeRepository.class);
    }
    
    @Test
    void shouldLoadLocalPostcodesFromFilesInLocation() {

        assertFalse(repository.hasPostcode(CaseInsensitiveId.createIdFor("EC1A1XH"))); // in london, outside area
        assertTrue(repository.hasPostcode(CaseInsensitiveId.createIdFor("WA141EP")));
        assertTrue(repository.hasPostcode(CaseInsensitiveId.createIdFor("wa141ep")));

        assertTrue(repository.hasPostcode(CaseInsensitiveId.createIdFor("M44BF"))); // central manchester
        assertTrue(repository.hasPostcode(CaseInsensitiveId.createIdFor(TestEnv.postcodeForWythenshaweHosp())));

        assertTrue(repository.hasPostcode(CaseInsensitiveId.createIdFor("WA160BE")));

    }


}
